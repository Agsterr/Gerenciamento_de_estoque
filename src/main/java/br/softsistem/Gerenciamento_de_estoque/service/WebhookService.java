package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Service para processar eventos de webhook do Stripe
 */
@Service
public class WebhookService {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    
    private final SubscriptionService subscriptionService;
    
    @Autowired
    public WebhookService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }
    
    /**
     * Processa eventos do Stripe baseado no tipo
     */
    public void processStripeEvent(Event event) {
        log.info("Processando evento: {} (ID: {})", event.getType(), event.getId());
        
        switch (event.getType()) {
            // Eventos de assinatura
            case "customer.subscription.created" -> handleSubscriptionCreated(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "customer.subscription.paused" -> handleSubscriptionPaused(event);
            case "customer.subscription.resumed" -> handleSubscriptionResumed(event);
            case "customer.subscription.trial_will_end" -> handleTrialWillEnd(event);
            case "customer.subscription.pending_update_applied" -> handleSubscriptionPendingUpdateApplied(event);
            case "customer.subscription.pending_update_expired" -> handleSubscriptionPendingUpdateExpired(event);
                
            // Eventos de fatura
            case "invoice.created" -> handleInvoiceCreated(event);
            case "invoice.finalized" -> handleInvoiceFinalized(event);
            case "invoice.paid" -> handleInvoicePaid(event);
            case "invoice.payment_succeeded" -> handlePaymentSucceeded(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            case "invoice.payment_action_required" -> handleInvoicePaymentActionRequired(event);
            case "invoice.overdue" -> handleInvoiceOverdue(event);
            case "invoice.marked_uncollectible" -> handleInvoiceMarkedUncollectible(event);
            case "invoice.voided" -> handleInvoiceVoided(event);
            case "invoice.upcoming" -> handleInvoiceUpcoming(event);
            case "invoice.will_be_due" -> handleInvoiceWillBeDue(event);
            case "invoice.sent" -> handleInvoiceSent(event);
            case "invoice.deleted" -> handleInvoiceDeleted(event);
            case "invoice.finalization_failed" -> handleInvoiceFinalizationFailed(event);
            case "invoice.overpaid" -> handleInvoiceOverpaid(event);
            case "invoice.updated" -> handleInvoiceUpdated(event);
                
            // Eventos de cobrança
            case "charge.succeeded" -> handleChargeSucceeded(event);
            case "charge.failed" -> handleChargeFailed(event);
            case "charge.captured" -> handleChargeCaptured(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            case "charge.updated" -> handleChargeUpdated(event);
            case "charge.pending" -> handleChargePending(event);
            case "charge.expired" -> handleChargeExpired(event);
                
            // Eventos de disputa
            case "charge.dispute.created" -> handleChargeDisputeCreated(event);
            case "charge.dispute.updated" -> handleChargeDisputeUpdated(event);
            case "charge.dispute.closed" -> handleChargeDisputeClosed(event);
            case "charge.dispute.funds_withdrawn" -> handleChargeDisputeFundsWithdrawn(event);
            case "charge.dispute.funds_reinstated" -> handleChargeDisputeFundsReinstated(event);
                
            // Eventos de reembolso
            case "refund.created" -> handleRefundCreated(event);
            case "refund.updated" -> handleRefundUpdated(event);
            case "refund.failed" -> handleRefundFailed(event);
            case "charge.refund.updated" -> handleChargeRefundUpdated(event);
                
            // Eventos de checkout
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "checkout.session.expired" -> handleCheckoutExpired(event);
            case "checkout.session.async_payment_succeeded" -> handleCheckoutAsyncPaymentSucceeded(event);
            case "checkout.session.async_payment_failed" -> handleCheckoutAsyncPaymentFailed(event);
                
            // Eventos de cliente
            case "customer.created" -> handleCustomerCreated(event);
            case "customer.updated" -> handleCustomerUpdated(event);
            case "customer.deleted" -> handleCustomerDeleted(event);
                
            // Eventos de desconto
            case "customer.discount.created" -> handleCustomerDiscountCreated(event);
            case "customer.discount.updated" -> handleCustomerDiscountUpdated(event);
            case "customer.discount.deleted" -> handleCustomerDiscountDeleted(event);
                
            // Eventos de métodos de pagamento
            case "customer.source.created" -> handleCustomerSourceCreated(event);
            case "customer.source.updated" -> handleCustomerSourceUpdated(event);
            case "customer.source.deleted" -> handleCustomerSourceDeleted(event);
            case "customer.source.expiring" -> handleCustomerSourceExpiring(event);
            case "customer.card.created" -> handleCustomerCardCreated(event);
            case "customer.card.updated" -> handleCustomerCardUpdated(event);
            case "customer.card.deleted" -> handleCustomerCardDeleted(event);
            case "customer.bank_account.created" -> handleCustomerBankAccountCreated(event);
            case "customer.bank_account.updated" -> handleCustomerBankAccountUpdated(event);
            case "customer.bank_account.deleted" -> handleCustomerBankAccountDeleted(event);
                
            // Eventos de tax ID
            case "customer.tax_id.created" -> handleCustomerTaxIdCreated(event);
            case "customer.tax_id.updated" -> handleCustomerTaxIdUpdated(event);
            case "customer.tax_id.deleted" -> handleCustomerTaxIdDeleted(event);
                
            // Eventos de payment intent
            case "payment_intent.created" -> handlePaymentIntentCreated(event);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentPaymentFailed(event);
            case "payment_intent.canceled" -> handlePaymentIntentCanceled(event);
            case "payment_intent.processing" -> handlePaymentIntentProcessing(event);
            case "payment_intent.requires_action" -> handlePaymentIntentRequiresAction(event);
            case "payment_intent.amount_capturable_updated" -> handlePaymentIntentAmountCapturableUpdated(event);
            case "payment_intent.partially_funded" -> handlePaymentIntentPartiallyFunded(event);
                
            // Eventos de payout
            case "payout.created" -> handlePayoutCreated(event);
            case "payout.paid" -> handlePayoutPaid(event);
            case "payout.failed" -> handlePayoutFailed(event);
            case "payout.canceled" -> handlePayoutCanceled(event);
            case "payout.updated" -> handlePayoutUpdated(event);
            case "payout.reconciliation_completed" -> handlePayoutReconciliationCompleted(event);
                
            // Eventos de plano e preço
            case "plan.created" -> handlePlanCreated(event);
            case "plan.updated" -> handlePlanUpdated(event);
            case "plan.deleted" -> handlePlanDeleted(event);
            case "price.created" -> handlePriceCreated(event);
            case "price.updated" -> handlePriceUpdated(event);
            case "price.deleted" -> handlePriceDeleted(event);
                
            // Eventos de subscription schedule
            case "subscription_schedule.created" -> handleSubscriptionScheduleCreated(event);
            case "subscription_schedule.updated" -> handleSubscriptionScheduleUpdated(event);
            case "subscription_schedule.canceled" -> handleSubscriptionScheduleCanceled(event);
            case "subscription_schedule.completed" -> handleSubscriptionScheduleCompleted(event);
            case "subscription_schedule.aborted" -> handleSubscriptionScheduleAborted(event);
            case "subscription_schedule.expiring" -> handleSubscriptionScheduleExpiring(event);
            case "subscription_schedule.released" -> handleSubscriptionScheduleReleased(event);
                
            // Eventos de verificação de identidade
            case "identity.verification_session.created" -> handleIdentityVerificationSessionCreated(event);
            case "identity.verification_session.processing" -> handleIdentityVerificationSessionProcessing(event);
            case "identity.verification_session.verified" -> handleIdentityVerificationSessionVerified(event);
            case "identity.verification_session.requires_input" -> handleIdentityVerificationSessionRequiresInput(event);
            case "identity.verification_session.canceled" -> handleIdentityVerificationSessionCanceled(event);
            case "identity.verification_session.redacted" -> handleIdentityVerificationSessionRedacted(event);
                
            // Eventos de pagamento de fatura
            case "invoice_payment.paid" -> handleInvoicePaymentPaid(event);
                
            default -> log.info("Evento não tratado: {}", event.getType());
        }
    }
    
    // ========== MÉTODOS DE PROCESSAMENTO DE EVENTOS ==========
    
    // Eventos de Assinatura
    private void handleSubscriptionCreated(Event event) {
        log.info("Processando criação de assinatura");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                
                String subscriptionId = subscription.getId();
                String customerId = subscription.getCustomer();
                String priceId = subscription.getItems().getData().get(0).getPrice().getId();
                
                LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                
                boolean isTrialing = subscription.getTrialEnd() != null && 
                    subscription.getTrialEnd() > Instant.now().getEpochSecond();
                
                subscriptionService.processSubscriptionCreated(
                    subscriptionId, customerId, priceId, 
                    currentPeriodStart, currentPeriodEnd, isTrialing
                );
                
                log.info("Assinatura criada processada: {}", subscriptionId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar criação de assinatura: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleSubscriptionUpdated(Event event) {
        log.info("Processando atualização de assinatura");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                
                String subscriptionId = subscription.getId();
                SubscriptionStatus status = mapStripeStatusToLocal(subscription.getStatus());
                
                LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                
                subscriptionService.updateSubscriptionFromStripe(
                    subscriptionId, status, currentPeriodStart, currentPeriodEnd
                );
                
                log.info("Assinatura atualizada processada: {} -> {}", subscriptionId, status);
            }
        } catch (Exception e) {
            log.error("Erro ao processar atualização de assinatura: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleSubscriptionDeleted(Event event) {
        log.info("Processando cancelamento de assinatura");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                
                String subscriptionId = subscription.getId();
                
                LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                
                subscriptionService.updateSubscriptionFromStripe(
                    subscriptionId, SubscriptionStatus.CANCELED, 
                    currentPeriodStart, currentPeriodEnd
                );
                
                log.info("Assinatura cancelada processada: {}", subscriptionId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar cancelamento de assinatura: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleSubscriptionPaused(Event event) {
        log.info("Processando pausa de assinatura - Event ID: {}", event.getId());
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                String subscriptionId = subscription.getId();
                String customerId = subscription.getCustomer();
                
                // Mapeia o status atual da assinatura do Stripe
                SubscriptionStatus pausedStatus = mapStripeStatusToLocal(subscription.getStatus());
                
                LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                
                // Atualiza a assinatura no sistema local
                subscriptionService.updateSubscriptionFromStripe(
                    subscriptionId, pausedStatus, currentPeriodStart, currentPeriodEnd
                );
                
                log.info("Assinatura pausada com sucesso: {} (Cliente: {}) - Status: {}", 
                    subscriptionId, customerId, pausedStatus);
                
                // Log adicional para auditoria
                log.warn("ATENÇÃO: Assinatura {} foi pausada. Acesso aos recursos pode ser limitado.", subscriptionId);
                
            } else {
                log.error("Objeto do evento não é uma Subscription válida para pausa");
            }
        } catch (Exception e) {
            log.error("Erro ao processar pausa de assinatura - Event ID: {}: {}", event.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleSubscriptionResumed(Event event) {
        log.info("Processando retomada de assinatura - Event ID: {}", event.getId());
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                String subscriptionId = subscription.getId();
                String customerId = subscription.getCustomer();
                
                // Mapeia o status atual da assinatura do Stripe
                SubscriptionStatus resumedStatus = mapStripeStatusToLocal(subscription.getStatus());
                
                LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                
                // Atualiza a assinatura no sistema local
                subscriptionService.updateSubscriptionFromStripe(
                    subscriptionId, resumedStatus, currentPeriodStart, currentPeriodEnd
                );
                
                log.info("Assinatura retomada com sucesso: {} (Cliente: {}) - Status: {}", 
                    subscriptionId, customerId, resumedStatus);
                
                // Log adicional para auditoria
                log.info("SUCESSO: Assinatura {} foi retomada. Acesso completo aos recursos restaurado.", subscriptionId);
                
            } else {
                log.error("Objeto do evento não é uma Subscription válida para retomada");
            }
        } catch (Exception e) {
            log.error("Erro ao processar retomada de assinatura - Event ID: {}: {}", event.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleTrialWillEnd(Event event) {
        log.info("Processando aviso de fim de trial");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                String subscriptionId = subscription.getId();
                log.info("Trial terminando em breve para assinatura: {}", subscriptionId);
                // Implementar notificação para o usuário
            }
        } catch (Exception e) {
            log.error("Erro ao processar aviso de fim de trial: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleSubscriptionPendingUpdateApplied(Event event) {
        log.info("Processando aplicação de atualização pendente de assinatura - Event ID: {}", event.getId());
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                String subscriptionId = subscription.getId();
                SubscriptionStatus status = mapStripeStatusToLocal(subscription.getStatus());
                
                LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                
                subscriptionService.updateSubscriptionFromStripe(
                    subscriptionId, status, currentPeriodStart, currentPeriodEnd
                );
                
                log.info("Atualização pendente aplicada para assinatura: {} -> {}", subscriptionId, status);
            }
        } catch (Exception e) {
            log.error("Erro ao processar aplicação de atualização pendente: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleSubscriptionPendingUpdateExpired(Event event) {
        log.info("Processando expiração de atualização pendente de assinatura - Event ID: {}", event.getId());
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Subscription subscription) {
                String subscriptionId = subscription.getId();
                
                // Quando uma atualização pendente expira, a assinatura mantém seu estado atual
                // Apenas logamos o evento para auditoria
                log.warn("Atualização pendente expirou para assinatura: {} - mantendo estado atual", subscriptionId);
                
                // Opcionalmente, podemos notificar o usuário sobre a expiração da atualização
                // ou implementar lógica específica de negócio aqui
            }
        } catch (Exception e) {
            log.error("Erro ao processar expiração de atualização pendente: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // Eventos de Pagamento
    private void handlePaymentSucceeded(Event event) {
        log.info("Processando pagamento bem-sucedido");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Invoice invoice) {
                
                if (invoice.getSubscription() != null) {
                    String subscriptionId = invoice.getSubscription();
                    
                    try {
                        Subscription subscription = Subscription.retrieve(subscriptionId);
                        
                        LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), ZoneId.systemDefault());
                        LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.systemDefault());
                        
                        subscriptionService.updateSubscriptionFromStripe(
                            subscriptionId, SubscriptionStatus.ACTIVE, 
                            currentPeriodStart, currentPeriodEnd
                        );
                        
                        log.info("Pagamento processado para assinatura: {} - Período: {} até {}", 
                            subscriptionId, currentPeriodStart, currentPeriodEnd);
                            
                    } catch (StripeException e) {
                        log.error("Erro ao buscar subscription {} via API do Stripe: {}", subscriptionId, e.getMessage());
                        
                        LocalDateTime now = LocalDateTime.now();
                        subscriptionService.updateSubscriptionFromStripe(
                            subscriptionId, SubscriptionStatus.ACTIVE, now, now.plusMonths(1)
                        );
                        
                        log.warn("Usando fallback para subscription {}: período estimado", subscriptionId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento bem-sucedido: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void handlePaymentFailed(Event event) {
        log.info("Processando falha de pagamento");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.Invoice invoice) {
                
                if (invoice.getSubscription() != null) {
                    String subscriptionId = invoice.getSubscription();
                    
                    LocalDateTime now = LocalDateTime.now();
                    subscriptionService.updateSubscriptionFromStripe(
                        subscriptionId, SubscriptionStatus.PAST_DUE, now, now
                    );
                    
                    log.warn("Falha de pagamento para assinatura: {}", subscriptionId);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar falha de pagamento: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // Eventos de Checkout
    private void handleCheckoutCompleted(Event event) {
        log.info("Processando checkout completado");
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof com.stripe.model.checkout.Session session) {
                
                String customerId = session.getCustomer();
                String subscriptionId = session.getSubscription();
                
                log.info("Checkout completado - Customer: {}, Subscription: {}", customerId, subscriptionId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar checkout completado: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // Métodos stub para todos os outros eventos
    private void handleInvoiceCreated(Event event) { log.info("Evento invoice.created processado - ID: {}", event.getId()); }
    private void handleInvoiceFinalized(Event event) { log.info("Evento invoice.finalized processado - ID: {}", event.getId()); }
    private void handleInvoicePaid(Event event) { log.info("Evento invoice.paid processado - ID: {}", event.getId()); }
    private void handleInvoicePaymentActionRequired(Event event) { log.info("Evento invoice.payment_action_required processado - ID: {}", event.getId()); }
    private void handleInvoiceOverdue(Event event) { log.info("Evento invoice.overdue processado - ID: {}", event.getId()); }
    private void handleInvoiceMarkedUncollectible(Event event) { log.info("Evento invoice.marked_uncollectible processado - ID: {}", event.getId()); }
    private void handleInvoiceVoided(Event event) { log.info("Evento invoice.voided processado - ID: {}", event.getId()); }
    private void handleInvoiceUpcoming(Event event) { log.info("Evento invoice.upcoming processado - ID: {}", event.getId()); }
    private void handleInvoiceWillBeDue(Event event) { log.info("Evento invoice.will_be_due processado - ID: {}", event.getId()); }
    private void handleInvoiceSent(Event event) { log.info("Evento invoice.sent processado - ID: {}", event.getId()); }
    private void handleInvoiceDeleted(Event event) { log.info("Evento invoice.deleted processado - ID: {}", event.getId()); }
    private void handleInvoiceFinalizationFailed(Event event) { log.info("Evento invoice.finalization_failed processado - ID: {}", event.getId()); }
    private void handleInvoiceOverpaid(Event event) { log.info("Evento invoice.overpaid processado - ID: {}", event.getId()); }
    private void handleInvoiceUpdated(Event event) { log.info("Evento invoice.updated processado - ID: {}", event.getId()); }
    
    private void handleChargeSucceeded(Event event) { log.info("Evento charge.succeeded processado - ID: {}", event.getId()); }
    private void handleChargeFailed(Event event) { log.info("Evento charge.failed processado - ID: {}", event.getId()); }
    private void handleChargeCaptured(Event event) { log.info("Evento charge.captured processado - ID: {}", event.getId()); }
    private void handleChargeRefunded(Event event) { log.info("Evento charge.refunded processado - ID: {}", event.getId()); }
    private void handleChargeUpdated(Event event) { log.info("Evento charge.updated processado - ID: {}", event.getId()); }
    private void handleChargePending(Event event) { log.info("Evento charge.pending processado - ID: {}", event.getId()); }
    private void handleChargeExpired(Event event) { log.info("Evento charge.expired processado - ID: {}", event.getId()); }
    
    private void handleChargeDisputeCreated(Event event) { log.info("Evento charge.dispute.created processado - ID: {}", event.getId()); }
    private void handleChargeDisputeUpdated(Event event) { log.info("Evento charge.dispute.updated processado - ID: {}", event.getId()); }
    private void handleChargeDisputeClosed(Event event) { log.info("Evento charge.dispute.closed processado - ID: {}", event.getId()); }
    private void handleChargeDisputeFundsWithdrawn(Event event) { log.info("Evento charge.dispute.funds_withdrawn processado - ID: {}", event.getId()); }
    private void handleChargeDisputeFundsReinstated(Event event) { log.info("Evento charge.dispute.funds_reinstated processado - ID: {}", event.getId()); }
    
    private void handleRefundCreated(Event event) { log.info("Evento refund.created processado - ID: {}", event.getId()); }
    private void handleRefundUpdated(Event event) { log.info("Evento refund.updated processado - ID: {}", event.getId()); }
    private void handleRefundFailed(Event event) { log.info("Evento refund.failed processado - ID: {}", event.getId()); }
    private void handleChargeRefundUpdated(Event event) { log.info("Evento charge.refund.updated processado - ID: {}", event.getId()); }
    
    private void handleCheckoutExpired(Event event) { log.info("Evento checkout.session.expired processado - ID: {}", event.getId()); }
    private void handleCheckoutAsyncPaymentSucceeded(Event event) { log.info("Evento checkout.session.async_payment_succeeded processado - ID: {}", event.getId()); }
    private void handleCheckoutAsyncPaymentFailed(Event event) { log.info("Evento checkout.session.async_payment_failed processado - ID: {}", event.getId()); }
    
    private void handleCustomerCreated(Event event) { log.info("Evento customer.created processado - ID: {}", event.getId()); }
    private void handleCustomerUpdated(Event event) { log.info("Evento customer.updated processado - ID: {}", event.getId()); }
    private void handleCustomerDeleted(Event event) { log.info("Evento customer.deleted processado - ID: {}", event.getId()); }
    
    private void handleCustomerDiscountCreated(Event event) { log.info("Evento customer.discount.created processado - ID: {}", event.getId()); }
    private void handleCustomerDiscountUpdated(Event event) { log.info("Evento customer.discount.updated processado - ID: {}", event.getId()); }
    private void handleCustomerDiscountDeleted(Event event) { log.info("Evento customer.discount.deleted processado - ID: {}", event.getId()); }
    
    private void handleCustomerSourceCreated(Event event) { log.info("Evento customer.source.created processado - ID: {}", event.getId()); }
    private void handleCustomerSourceUpdated(Event event) { log.info("Evento customer.source.updated processado - ID: {}", event.getId()); }
    private void handleCustomerSourceDeleted(Event event) { log.info("Evento customer.source.deleted processado - ID: {}", event.getId()); }
    private void handleCustomerSourceExpiring(Event event) { log.info("Evento customer.source.expiring processado - ID: {}", event.getId()); }
    
    private void handleCustomerCardCreated(Event event) { log.info("Evento customer.card.created processado - ID: {}", event.getId()); }
    private void handleCustomerCardUpdated(Event event) { log.info("Evento customer.card.updated processado - ID: {}", event.getId()); }
    private void handleCustomerCardDeleted(Event event) { log.info("Evento customer.card.deleted processado - ID: {}", event.getId()); }
    
    private void handleCustomerBankAccountCreated(Event event) { log.info("Evento customer.bank_account.created processado - ID: {}", event.getId()); }
    private void handleCustomerBankAccountUpdated(Event event) { log.info("Evento customer.bank_account.updated processado - ID: {}", event.getId()); }
    private void handleCustomerBankAccountDeleted(Event event) { log.info("Evento customer.bank_account.deleted processado - ID: {}", event.getId()); }
    
    private void handleCustomerTaxIdCreated(Event event) { log.info("Evento customer.tax_id.created processado - ID: {}", event.getId()); }
    private void handleCustomerTaxIdUpdated(Event event) { log.info("Evento customer.tax_id.updated processado - ID: {}", event.getId()); }
    private void handleCustomerTaxIdDeleted(Event event) { log.info("Evento customer.tax_id.deleted processado - ID: {}", event.getId()); }
    
    private void handlePaymentIntentCreated(Event event) { log.info("Evento payment_intent.created processado - ID: {}", event.getId()); }
    private void handlePaymentIntentSucceeded(Event event) { log.info("Evento payment_intent.succeeded processado - ID: {}", event.getId()); }
    private void handlePaymentIntentPaymentFailed(Event event) { log.info("Evento payment_intent.payment_failed processado - ID: {}", event.getId()); }
    private void handlePaymentIntentCanceled(Event event) { log.info("Evento payment_intent.canceled processado - ID: {}", event.getId()); }
    private void handlePaymentIntentProcessing(Event event) { log.info("Evento payment_intent.processing processado - ID: {}", event.getId()); }
    private void handlePaymentIntentRequiresAction(Event event) { log.info("Evento payment_intent.requires_action processado - ID: {}", event.getId()); }
    private void handlePaymentIntentAmountCapturableUpdated(Event event) { log.info("Evento payment_intent.amount_capturable_updated processado - ID: {}", event.getId()); }
    private void handlePaymentIntentPartiallyFunded(Event event) { log.info("Evento payment_intent.partially_funded processado - ID: {}", event.getId()); }
    
    private void handlePayoutCreated(Event event) { log.info("Evento payout.created processado - ID: {}", event.getId()); }
    private void handlePayoutPaid(Event event) { log.info("Evento payout.paid processado - ID: {}", event.getId()); }
    private void handlePayoutFailed(Event event) { log.info("Evento payout.failed processado - ID: {}", event.getId()); }
    private void handlePayoutCanceled(Event event) { log.info("Evento payout.canceled processado - ID: {}", event.getId()); }
    private void handlePayoutUpdated(Event event) { log.info("Evento payout.updated processado - ID: {}", event.getId()); }
    private void handlePayoutReconciliationCompleted(Event event) { log.info("Evento payout.reconciliation_completed processado - ID: {}", event.getId()); }
    
    private void handlePlanCreated(Event event) { log.info("Evento plan.created processado - ID: {}", event.getId()); }
    private void handlePlanUpdated(Event event) { log.info("Evento plan.updated processado - ID: {}", event.getId()); }
    private void handlePlanDeleted(Event event) { log.info("Evento plan.deleted processado - ID: {}", event.getId()); }
    
    private void handlePriceCreated(Event event) { log.info("Evento price.created processado - ID: {}", event.getId()); }
    private void handlePriceUpdated(Event event) { log.info("Evento price.updated processado - ID: {}", event.getId()); }
    private void handlePriceDeleted(Event event) { log.info("Evento price.deleted processado - ID: {}", event.getId()); }
    
    private void handleSubscriptionScheduleCreated(Event event) { log.info("Evento subscription_schedule.created processado - ID: {}", event.getId()); }
    private void handleSubscriptionScheduleUpdated(Event event) { log.info("Evento subscription_schedule.updated processado - ID: {}", event.getId()); }
    private void handleSubscriptionScheduleCanceled(Event event) { log.info("Evento subscription_schedule.canceled processado - ID: {}", event.getId()); }
    private void handleSubscriptionScheduleCompleted(Event event) { log.info("Evento subscription_schedule.completed processado - ID: {}", event.getId()); }
    private void handleSubscriptionScheduleAborted(Event event) { log.info("Evento subscription_schedule.aborted processado - ID: {}", event.getId()); }
    private void handleSubscriptionScheduleExpiring(Event event) { log.info("Evento subscription_schedule.expiring processado - ID: {}", event.getId()); }
    private void handleSubscriptionScheduleReleased(Event event) { log.info("Evento subscription_schedule.released processado - ID: {}", event.getId()); }
    
    private void handleIdentityVerificationSessionCreated(Event event) { log.info("Evento identity.verification_session.created processado - ID: {}", event.getId()); }
    private void handleIdentityVerificationSessionProcessing(Event event) { log.info("Evento identity.verification_session.processing processado - ID: {}", event.getId()); }
    private void handleIdentityVerificationSessionVerified(Event event) { log.info("Evento identity.verification_session.verified processado - ID: {}", event.getId()); }
    private void handleIdentityVerificationSessionRequiresInput(Event event) { log.info("Evento identity.verification_session.requires_input processado - ID: {}", event.getId()); }
    private void handleIdentityVerificationSessionCanceled(Event event) { log.info("Evento identity.verification_session.canceled processado - ID: {}", event.getId()); }
    private void handleIdentityVerificationSessionRedacted(Event event) { log.info("Evento identity.verification_session.redacted processado - ID: {}", event.getId()); }
    
    private void handleInvoicePaymentPaid(Event event) { log.info("Evento invoice_payment.paid processado - ID: {}", event.getId()); }
    
    /**
     * Mapeia status do Stripe para status local
     */
    private SubscriptionStatus mapStripeStatusToLocal(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIAL;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED;
            default -> {
                log.warn("Status desconhecido do Stripe: {}", stripeStatus);
                yield SubscriptionStatus.EXPIRED;
            }
        };
    }
}