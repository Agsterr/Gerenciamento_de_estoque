package br.softsistem.Gerenciamento_de_estoque.service.webhook;

import br.softsistem.Gerenciamento_de_estoque.service.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Handler específico para eventos de pedido do comerciante (merchant_order)
 * 
 * Requisitos:
 * - Buscar a ordem comercial pelo ID
 * - Somar pagamentos aprovados
 * - Comparar com total da ordem
 * - Marcar pedido como: PAGO, PARCIAL, NÃO PAGO
 * - Só liberar pedido quando totalmente pago
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
public class MerchantOrderWebhookHandler implements WebhookEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(MerchantOrderWebhookHandler.class);
    
    private final MercadoPagoService mercadoPagoService;
    
    @Autowired
    public MerchantOrderWebhookHandler(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }
    
    @Override
    public void handle(String dataId, Map<String, Object> payload, Map<String, Object> notificationData) throws Exception {
        log.info("=== Processando evento de MERCHANT_ORDER ===");
        log.info("Order ID: {}", dataId);
        
        try {
            // 1. Buscar a ordem comercial pelo ID na API do Mercado Pago
            Long orderId;
            try {
                orderId = Long.parseLong(dataId);
            } catch (NumberFormatException e) {
                log.error("✗ Order ID inválido: {}", dataId);
                throw new RuntimeException("Order ID inválido: " + dataId);
            }
            
            com.mercadopago.resources.merchantorder.MerchantOrder merchantOrder;
            try {
                merchantOrder = mercadoPagoService.getMerchantOrder(orderId);
                log.info("✓ Ordem comercial {} obtida via API do Mercado Pago", orderId);
            } catch (MPException | MPApiException e) {
                log.error("✗ Erro ao buscar ordem comercial {} via API: {}", orderId, e.getMessage(), e);
                throw new RuntimeException("Erro ao buscar ordem comercial: " + e.getMessage(), e);
            }
            
            // 2. Obter total da ordem
            BigDecimal totalAmount = extractTotalAmount(merchantOrder);
            log.info("Total da ordem: {}", totalAmount);
            
            // 3. Somar pagamentos aprovados
            BigDecimal paidAmount = sumApprovedPayments(merchantOrder);
            log.info("Valor pago (aprovado): {}", paidAmount);
            
            // 4. Comparar com total da ordem e marcar status
            OrderPaymentStatus status = determineOrderStatus(totalAmount, paidAmount);
            log.info("Status da ordem: {}", status);
            
            // 5. Só liberar pedido quando totalmente pago
            if (status == OrderPaymentStatus.PAGO) {
                log.info("✓ Ordem totalmente paga - liberando pedido");
                releaseOrder(merchantOrder);
            } else if (status == OrderPaymentStatus.PARCIAL) {
                log.warn("⚠ Ordem parcialmente paga - aguardando pagamento completo");
                log.warn("Faltam: {} para completar o pagamento", totalAmount.subtract(paidAmount));
            } else {
                log.warn("✗ Ordem não paga - pedido NÃO será liberado");
            }
            
            log.info("=== Processamento de MERCHANT_ORDER concluído ===");
            
        } catch (Exception e) {
            log.error("✗ Erro ao processar merchant_order: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Extrai o valor total da ordem
     */
    private BigDecimal extractTotalAmount(com.mercadopago.resources.merchantorder.MerchantOrder merchantOrder) {
        try {
            Object totalAmountObj = merchantOrder.getTotalAmount();
            if (totalAmountObj != null) {
                if (totalAmountObj instanceof Double) {
                    return BigDecimal.valueOf((Double) totalAmountObj);
                } else if (totalAmountObj instanceof BigDecimal) {
                    return (BigDecimal) totalAmountObj;
                } else if (totalAmountObj instanceof Number) {
                    return BigDecimal.valueOf(((Number) totalAmountObj).doubleValue());
                }
            }
        } catch (Exception e) {
            log.debug("TotalAmount não disponível: {}", e.getMessage());
        }
        
        // Fallback: somar itens
        try {
            List<com.mercadopago.resources.merchantorder.MerchantOrderItem> items = merchantOrder.getItems();
            if (items != null && !items.isEmpty()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (com.mercadopago.resources.merchantorder.MerchantOrderItem item : items) {
                    Object unitPriceObj = item.getUnitPrice();
                    Object quantityObj = item.getQuantity();
                    
                    BigDecimal unitPrice = extractBigDecimal(unitPriceObj);
                    BigDecimal quantity = extractBigDecimal(quantityObj);
                    
                    if (unitPrice != null && quantity != null) {
                        sum = sum.add(unitPrice.multiply(quantity));
                    }
                }
                return sum;
            }
        } catch (Exception e) {
            log.debug("Erro ao calcular total dos itens: {}", e.getMessage());
        }
        
        log.warn("Não foi possível determinar o total da ordem");
        return BigDecimal.ZERO;
    }
    
    /**
     * Soma os pagamentos aprovados
     */
    private BigDecimal sumApprovedPayments(com.mercadopago.resources.merchantorder.MerchantOrder merchantOrder) {
        BigDecimal sum = BigDecimal.ZERO;
        
        try {
            List<com.mercadopago.resources.merchantorder.MerchantOrderPayment> payments = merchantOrder.getPayments();
            if (payments != null) {
                for (com.mercadopago.resources.merchantorder.MerchantOrderPayment payment : payments) {
                    // Verificar se o pagamento está aprovado
                    String status = payment.getStatus() != null ? payment.getStatus().toString() : null;
                    if ("approved".equalsIgnoreCase(status)) {
                        Object transactionAmountObj = payment.getTransactionAmount();
                        BigDecimal amount = extractBigDecimal(transactionAmountObj);
                        if (amount != null) {
                            sum = sum.add(amount);
                            log.debug("Pagamento aprovado adicionado: {}", amount);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao somar pagamentos aprovados: {}", e.getMessage());
        }
        
        return sum;
    }
    
    /**
     * Extrai BigDecimal de um objeto
     */
    private BigDecimal extractBigDecimal(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Double) {
            return BigDecimal.valueOf((Double) obj);
        } else if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        } else if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        return null;
    }
    
    /**
     * Determina o status do pagamento da ordem
     */
    private OrderPaymentStatus determineOrderStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return OrderPaymentStatus.NAO_PAGO;
        }
        
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        
        int comparison = paidAmount.compareTo(totalAmount);
        
        if (comparison >= 0) {
            // Pago totalmente ou mais
            return OrderPaymentStatus.PAGO;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Parcialmente pago
            return OrderPaymentStatus.PARCIAL;
        } else {
            // Não pago
            return OrderPaymentStatus.NAO_PAGO;
        }
    }
    
    /**
     * Libera o pedido quando totalmente pago
     */
    private void releaseOrder(com.mercadopago.resources.merchantorder.MerchantOrder merchantOrder) {
        log.info("Liberando pedido da ordem: {}", merchantOrder.getId());
        // TODO: Implementar lógica específica de liberação do pedido
        // Exemplo: atualizar status no banco de dados, enviar notificação, etc.
    }
    
    /**
     * Enum para status de pagamento da ordem
     */
    public enum OrderPaymentStatus {
        PAGO,      // Totalmente pago
        PARCIAL,   // Parcialmente pago
        NAO_PAGO   // Não pago
    }
    
    @Override
    public String getEventType() {
        return "merchant_order";
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return "merchant_order".equalsIgnoreCase(eventType);
    }
}

