package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.webhook.WebhookResponseDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.exception.InvalidSignatureException;
import br.softsistem.Gerenciamento_de_estoque.exception.WebhookProcessingException;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.MercadoPagoWebhookEventType;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.model.WebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.repository.PaymentRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.WebhookEventRepository;
import br.softsistem.Gerenciamento_de_estoque.service.webhook.WebhookEventHandler;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service para processar eventos de webhook do Mercado Pago
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final SubscriptionService subscriptionService;
    private final MercadoPagoConfig mercadoPagoConfig;
    private final MercadoPagoService mercadoPagoService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookSignatureValidator signatureValidator;
    private final java.util.List<WebhookEventHandler> eventHandlers;
    private final FailedWebhookEventService failedWebhookEventService;
    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;
    private final br.softsistem.Gerenciamento_de_estoque.config.WebhookConfig webhookConfig;
    private final WebhookMonitoringService monitoringService;
    private final WebhookAlertService alertService;

    @Autowired
    public WebhookService(SubscriptionService subscriptionService,
            MercadoPagoConfig mercadoPagoConfig,
            @Autowired(required = false) MercadoPagoService mercadoPagoService,
            PaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository,
            WebhookEventRepository webhookEventRepository,
            WebhookSignatureValidator signatureValidator,
            @Lazy java.util.List<WebhookEventHandler> eventHandlers,
            FailedWebhookEventService failedWebhookEventService,
            UsuarioRepository usuarioRepository,
            PlanRepository planRepository,
            br.softsistem.Gerenciamento_de_estoque.config.WebhookConfig webhookConfig,
            WebhookMonitoringService monitoringService,
            WebhookAlertService alertService) {
        this.subscriptionService = subscriptionService;
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.mercadoPagoService = mercadoPagoService;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.signatureValidator = signatureValidator;
        this.eventHandlers = eventHandlers;
        this.failedWebhookEventService = failedWebhookEventService;
        this.usuarioRepository = usuarioRepository;
        this.planRepository = planRepository;
        this.webhookConfig = webhookConfig;
        this.monitoringService = monitoringService;
        this.alertService = alertService;
    }

    /**
     * Retorna o webhook secret para validação
     */
    public String getWebhookSecret() {
        return mercadoPagoConfig.getWebhookSecret();
    }

    // ========== MÉTODOS DE PROCESSAMENTO DE EVENTOS ==========

    /**
     * Valida assinatura do webhook (método público para uso no controller)
     * 
     * Utiliza a classe WebhookSignatureValidator para validação conforme
     * documentação oficial
     */
    public boolean validateWebhookSignature(String signature, String requestBody, String secret) {
        return signatureValidator.validate(signature, requestBody, secret);
    }

    /**
     * Processa webhook de forma assíncrona (não bloqueia a resposta HTTP)
     * 
     * Este método é executado em background após o controller retornar HTTP 200
     * 
     * Mecanismos de segurança implementados:
     * - Timeout controlado
     * - Logs estruturados
     * - Tratamento de falhas sem quebrar endpoint
     * - Salvamento garantido de event_id
     */
    @Async("webhookTaskExecutor")
    public void processWebhookAsync(
            Map<String, Object> payload,
            String signature,
            String requestId,
            String requestBody) {

        long startTime = System.currentTimeMillis();
        String eventId = extractEventIdSafely(payload, requestId);
        String eventType = extractEventTypeSafely(payload);

        // Log estruturado inicial
        logStructured("webhook.processing.started", Map.of(
                "requestId", requestId != null ? requestId : "unknown",
                "eventId", eventId,
                "eventType", eventType != null ? eventType : "unknown",
                "timestamp", LocalDateTime.now().toString()));

        try {
            // Processar com timeout controlado
            handleMercadoPagoWebhookWithTimeout(payload, signature, requestId, requestBody, eventId);

            long duration = System.currentTimeMillis() - startTime;

            // Log estruturado de sucesso
            logStructured("webhook.processing.success", Map.of(
                    "requestId", requestId != null ? requestId : "unknown",
                    "eventId", eventId,
                    "eventType", eventType != null ? eventType : "unknown",
                    "durationMs", String.valueOf(duration),
                    "timestamp", LocalDateTime.now().toString()));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Log estruturado de erro
            logStructured("webhook.processing.error", Map.of(
                    "requestId", requestId != null ? requestId : "unknown",
                    "eventId", eventId,
                    "eventType", eventType != null ? eventType : "unknown",
                    "error", e.getClass().getSimpleName(),
                    "errorMessage", e.getMessage() != null ? e.getMessage() : "unknown",
                    "durationMs", String.valueOf(duration),
                    "timestamp", LocalDateTime.now().toString()));

            // Salvar evento falhado SEM lançar exceção (não quebra endpoint)
            saveFailedEventSafely(eventId, eventType, payload, e);
        }
    }

    /**
     * Processa webhook com timeout controlado
     */
    private void handleMercadoPagoWebhookWithTimeout(
            Map<String, Object> payload,
            String signature,
            String requestId,
            String requestBody,
            String eventId) throws Exception {

        // Garantir que event_id seja salvo ANTES de processar
        ensureEventIdSaved(eventId);

        // Processar com timeout controlado usando CompletableFuture
        try {
            CompletableFuture<WebhookResponseDto> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return handleMercadoPagoWebhook(payload, signature, requestId, requestBody);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Aguardar com timeout
            long timeoutMillis = webhookConfig.getProcessingTimeoutMillis();
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            log.error("Timeout ao processar webhook - Request ID: {}, Event ID: {}, Timeout: {}ms",
                    requestId, eventId, webhookConfig.getProcessingTimeoutMillis());
            throw new WebhookProcessingException(
                    "Timeout ao processar webhook após " + webhookConfig.getProcessingTimeoutSeconds() + " segundos",
                    requestId,
                    extractEventTypeSafely(payload),
                    e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && cause.getCause() != null) {
                throw (Exception) cause.getCause();
            }
            throw new WebhookProcessingException(
                    "Erro ao processar webhook: " + e.getMessage(),
                    requestId,
                    extractEventTypeSafely(payload),
                    e);
        }
    }

    /**
     * Garante que event_id seja salvo no banco de dados
     * Isso garante idempotência mesmo se o processamento falhar
     */
    private void ensureEventIdSaved(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            log.warn("Event ID vazio - não é possível garantir idempotência");
            return;
        }

        try {
            // Verificar se já existe
            Optional<WebhookEvent> existingOpt = webhookEventRepository.findByEventId(eventId);
            if (existingOpt.isEmpty()) {
                // Salvar event_id ANTES de processar com processed = false
                WebhookEvent webhookEvent = new WebhookEvent(eventId);
                webhookEvent.setProcessed(false);
                webhookEventRepository.save(webhookEvent);

                logStructured("webhook.event.saved", Map.of(
                        "eventId", eventId,
                        "processed", "false",
                        "timestamp", LocalDateTime.now().toString()));
            } else {
                logStructured("webhook.event.duplicate", Map.of(
                        "eventId", eventId,
                        "timestamp", LocalDateTime.now().toString()));
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race condition: outro thread já salvou
            log.debug("Event ID {} já foi salvo por outro thread", eventId);
        } catch (Exception e) {
            log.error("Erro ao salvar event_id {}: {}", eventId, e.getMessage(), e);
            // Não lançar exceção - apenas logar
        }
    }

    /**
     * Extrai event_id de forma segura
     */
    private String extractEventIdSafely(Map<String, Object> payload, String fallback) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null) {
                Object idObj = data.get("id");
                if (idObj != null) {
                    return idObj.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair eventId: {}", e.getMessage());
        }
        return fallback != null ? fallback : "unknown";
    }

    /**
     * Extrai event_type de forma segura
     */
    private String extractEventTypeSafely(Map<String, Object> payload) {
        try {
            Object typeObj = payload.get("type");
            if (typeObj != null) {
                return typeObj.toString();
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair eventType: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Salva evento falhado de forma segura (não lança exceção)
     */
    private void saveFailedEventSafely(String eventId, String eventType, Map<String, Object> payload, Exception error) {
        try {
            saveFailedEvent(eventId, eventType, payload, error);
        } catch (Exception e) {
            // Logar mas não lançar exceção - endpoint não deve quebrar
            log.error("Erro crítico ao salvar evento falhado (eventId={}): {}", eventId, e.getMessage(), e);
        }
    }

    /**
     * Log estruturado para melhor rastreabilidade
     * Formato: [WEBHOOK] key=value key=value ...
     */
    private void logStructured(String event, Map<String, String> fields) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[WEBHOOK] ");
        logMessage.append("event=").append(event);

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            logMessage.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }

        switch (event) {
            case "webhook.processing.started":
            case "webhook.processing.success":
                log.info(logMessage.toString());
                break;
            case "webhook.event.saved":
                log.debug(logMessage.toString());
                break;
            case "webhook.event.duplicate":
            case "webhook.event.duplicate.detected":
            case "webhook.event.duplicate.race":
                log.warn(logMessage.toString());
                break;
            case "webhook.validation.failed":
            case "webhook.processing.exception":
            case "webhook.processing.error":
                log.error(logMessage.toString());
                break;
            default:
                log.info(logMessage.toString());
        }
    }

    /**
     * Processa webhooks do Mercado Pago (método síncrono para uso interno)
     * O Mercado Pago envia notificações no formato: { "type": "...", "data": {
     * "id": "..." } }
     * 
     * Conforme documentação oficial (2025):
     * - Header X-Signature: ts=<timestamp>,v1=<hash>
     * - Header X-Request-Id: ID único da requisição (para idempotência)
     * - Payload: { "type": "payment", "data": { "id": "123456789" } }
     * 
     * @param payload     Payload do webhook
     * @param signature   Header x-signature para validação
     * @param requestId   Header x-request-id para idempotência
     * @param requestBody Body raw da requisição (necessário para validação de
     *                    assinatura)
     * @return WebhookResponseDto com resultado do processamento
     */
    @Transactional
    public WebhookResponseDto handleMercadoPagoWebhook(
            Map<String, Object> payload,
            String signature,
            String requestId,
            String requestBody) {
        log.info("Recebido webhook do Mercado Pago - Request ID: {}", requestId);

        try {
            boolean isReprocess = requestId != null && requestId.startsWith("reprocess-");
            if (!isReprocess) {
                String webhookSecret = mercadoPagoConfig.getWebhookSecret();
                if (!signatureValidator.validate(signature, requestBody, webhookSecret)) {
                    log.error("Webhook rejeitado - assinatura inválida");
                    throw new InvalidSignatureException("Assinatura inválida - webhook rejeitado", signature, null);
                }
            }

            // Extrair tipo e data_id do payload
            String type = (String) payload.get("type");
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (type == null || data == null) {
                log.error("Payload do webhook inválido: tipo ou data ausente");
                throw new WebhookProcessingException("Payload do webhook inválido", null, null, null);
            }

            // IMPORTANTE: eventId vem do campo "id" do webhook (não do payment_id)
            // Exemplo: payload { "type": "payment", "data": { "id": "123456789" } }
            // eventId será "123456789"
            String eventId = (String) data.get("id");
            if (eventId == null || eventId.isEmpty()) {
                log.error("Event ID ausente no webhook - campo 'id' não encontrado em data");
                throw new WebhookProcessingException("Event ID ausente no webhook", null, null, null);
            }

            // Verificar idempotência persistente usando banco de dados
            // IMPORTANTE: Verificação dupla para garantir idempotência mesmo em race
            // conditions
            if (webhookEventRepository.existsByEventId(eventId)) {
                logStructured("webhook.event.duplicate.detected", Map.of(
                        "eventId", eventId,
                        "eventType", type,
                        "requestId", requestId != null ? requestId : "unknown",
                        "timestamp", LocalDateTime.now().toString()));
                return WebhookResponseDto.alreadyProcessed(eventId, type);
            }

            // Persistir evento ANTES de processar (garante idempotência mesmo em caso de
            // erro)
            // IMPORTANTE: Salvar com processed = false - será marcado como true após
            // processamento bem-sucedido
            WebhookEvent webhookEvent;
            try {
                Optional<WebhookEvent> existingEventOpt = webhookEventRepository.findByEventId(eventId);
                if (existingEventOpt.isPresent()) {
                    webhookEvent = existingEventOpt.get();
                    // Se já existe e está processado, retornar
                    if (Boolean.TRUE.equals(webhookEvent.getProcessed())) {
                        logStructured("webhook.event.duplicate.detected", Map.of(
                                "eventId", eventId,
                                "eventType", type,
                                "requestId", requestId != null ? requestId : "unknown",
                                "timestamp", LocalDateTime.now().toString()));
                        return WebhookResponseDto.alreadyProcessed(eventId, type);
                    }
                    // Se existe mas não está processado, usar o existente (pode ser retry)
                    log.info("Evento {} encontrado mas não processado, tentando reprocessar", eventId);
                } else {
                    // Criar novo evento com processed = false
                    webhookEvent = new WebhookEvent(eventId);
                    webhookEvent.setProcessed(false);
                    webhookEventRepository.save(webhookEvent);
                }

                logStructured("webhook.event.registered", Map.of(
                        "eventId", eventId,
                        "eventType", type,
                        "requestId", requestId != null ? requestId : "unknown",
                        "processed", "false",
                        "timestamp", LocalDateTime.now().toString()));
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Caso raro: evento foi inserido entre a verificação e o save (race condition)
                // Isso é normal em ambientes com múltiplas instâncias ou processamento
                // concorrente
                logStructured("webhook.event.duplicate.race", Map.of(
                        "eventId", eventId,
                        "eventType", type,
                        "requestId", requestId != null ? requestId : "unknown",
                        "timestamp", LocalDateTime.now().toString()));
                return WebhookResponseDto.alreadyProcessed(eventId, type);
            }

            // Registrar recebimento no monitoramento
            monitoringService.recordWebhookReceived(type);

            // Processar evento baseado no tipo
            // dataId é usado para buscar detalhes do pagamento via API
            String dataId = eventId; // Para compatibilidade com métodos existentes

            logStructured("webhook.processing.event", Map.of(
                    "eventId", eventId,
                    "eventType", type,
                    "dataId", dataId,
                    "requestId", requestId != null ? requestId : "unknown",
                    "timestamp", LocalDateTime.now().toString()));

            processMercadoPagoEvent(type, dataId, payload);

            // Marcar evento como processado com sucesso
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEvent.setErrorMessage(null); // Limpar erro anterior se houver
            webhookEventRepository.save(webhookEvent);

            // Registrar processamento bem-sucedido no monitoramento
            monitoringService.recordWebhookProcessed(type);

            logStructured("webhook.processing.completed", Map.of(
                    "eventId", eventId,
                    "eventType", type,
                    "requestId", requestId != null ? requestId : "unknown",
                    "processed", "true",
                    "timestamp", LocalDateTime.now().toString()));

            return WebhookResponseDto.success(eventId, type);

        } catch (InvalidSignatureException e) {
            // Re-lançar exceção de assinatura inválida (não deve ser processada)
            logStructured("webhook.validation.failed", Map.of(
                    "requestId", requestId != null ? requestId : "unknown",
                    "error", "InvalidSignatureException",
                    "errorMessage", e.getMessage() != null ? e.getMessage() : "unknown",
                    "timestamp", LocalDateTime.now().toString()));
            throw e;
        } catch (Exception e) {
            String extractedEventId = extractEventIdSafely(payload, requestId);
            String extractedEventType = extractEventTypeSafely(payload);

            // Log estruturado do erro
            logStructured("webhook.processing.exception", Map.of(
                    "requestId", requestId != null ? requestId : "unknown",
                    "eventId", extractedEventId,
                    "eventType", extractedEventType,
                    "error", e.getClass().getSimpleName(),
                    "errorMessage", e.getMessage() != null ? e.getMessage() : "unknown",
                    "timestamp", LocalDateTime.now().toString()));

            // Atualizar webhookEvent com erro (mantém processed = false para permitir
            // retry)
            try {
                Optional<WebhookEvent> eventOpt = webhookEventRepository.findByEventId(extractedEventId);
                if (eventOpt.isPresent()) {
                    WebhookEvent failedEvent = eventOpt.get();
                    failedEvent.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Erro desconhecido");
                    webhookEventRepository.save(failedEvent);
                    log.debug("Evento {} marcado com erro para retry futuro", extractedEventId);
                }
            } catch (Exception saveError) {
                log.error("Erro ao salvar mensagem de erro no webhookEvent: {}", saveError.getMessage());
            }

            // Salvar evento falhado SEM lançar exceção adicional
            saveFailedEventSafely(
                    extractedEventId,
                    extractedEventType,
                    payload,
                    e);

            throw new WebhookProcessingException(
                    "Erro ao processar webhook: " + e.getMessage(),
                    requestId,
                    extractedEventType,
                    e);
        }
    }

    /**
     * Processa eventos do Mercado Pago baseado no tipo usando Strategy Pattern
     * 
     * Cada tipo de evento é redirecionado para seu handler específico,
     * garantindo separação de responsabilidades e não misturar regras de negócio.
     */
    private void processMercadoPagoEvent(String type, String dataId, Map<String, Object> payload) {
        log.info("Processando evento Mercado Pago: {} (ID: {})", type, dataId);

        // Verificar se o tipo de evento é suportado
        MercadoPagoWebhookEventType eventType = MercadoPagoWebhookEventType.fromString(type);
        if (eventType == null) {
            log.warn("Tipo de evento não suportado: {}", type);
            return;
        }

        try {
            // Buscar dados da notificação via API do Mercado Pago
            Map<String, Object> notificationData = mercadoPagoService.processWebhookNotification(dataId, type);

            // Encontrar handler apropriado para o tipo de evento
            WebhookEventHandler handler = findHandlerForEventType(type);
            if (handler != null) {
                log.info("Usando handler {} para evento {}", handler.getClass().getSimpleName(), type);
                handler.handle(dataId, payload, notificationData);
            } else {
                log.error("Handler não encontrado para tipo de evento: {}", type);
                throw new WebhookProcessingException("Handler não encontrado para tipo: " + type, dataId, type, null);
            }
        } catch (MPException | MPApiException e) {
            log.error("Erro ao processar evento do Mercado Pago: {}", e.getMessage(), e);
            // Salvar evento falhado antes de lançar exceção
            saveFailedEvent(dataId, type, payload, e);
            throw new WebhookProcessingException("Erro ao processar evento: " + e.getMessage(), dataId, type, e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar evento: {}", e.getMessage(), e);
            // Salvar evento falhado antes de lançar exceção
            saveFailedEvent(dataId, type, payload, e);
            throw new WebhookProcessingException("Erro inesperado: " + e.getMessage(), dataId, type, e);
        }
    }

    /**
     * Salva evento que falhou no processamento
     */
    private void saveFailedEvent(String eventId, String eventType, Map<String, Object> payload, Exception error) {
        try {
            // Converter payload para JSON string
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);
            failedWebhookEventService.saveFailedEvent(eventId, eventType, payloadJson, error);
        } catch (Exception e) {
            log.error("Erro ao salvar evento falhado: {}", e.getMessage(), e);
            // Tentar salvar com payload vazio
            try {
                failedWebhookEventService.saveFailedEvent(eventId, eventType, "{}", error);
            } catch (Exception e2) {
                log.error("Erro crítico ao salvar evento falhado mesmo com payload vazio: {}", e2.getMessage());
            }
        }
    }

    /**
     * Encontra o handler apropriado para o tipo de evento
     */
    private WebhookEventHandler findHandlerForEventType(String eventType) {
        return eventHandlers.stream()
                .filter(handler -> handler.canHandle(eventType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Método interno para processamento de pagamento (chamado pelo
     * PaymentWebhookHandler)
     * 
     * @param paymentData Dados do pagamento
     */
    public void handleMercadoPagoPaymentInternal(Map<String, Object> paymentData) {
        handleMercadoPagoPayment(paymentData);
    }

    /**
     * Método interno para processamento de assinatura (chamado pelo
     * SubscriptionWebhookHandler)
     * 
     * @param preapprovalData Dados da assinatura
     */
    public void handleMercadoPagoPreApprovalInternal(Map<String, Object> preapprovalData) {
        String action = null;
        try {
            Object actionObj = preapprovalData.get("action");
            if (actionObj != null) {
                action = actionObj.toString();
            }
        } catch (Exception ignored) {
        }
        if (action == null || action.isBlank()) {
            action = "updated";
        }
        handleSubscriptionPreapproval(preapprovalData, action);
    }

    /**
     * Processa pagamento do Mercado Pago seguindo fluxo obrigatório:
     * 
     * 1. Receber webhook ✓
     * 2. Validar assinatura ✓ (já validado antes de chegar aqui)
     * 3. Buscar pagamento na API Mercado Pago (GET /v1/payments/{id})
     * 4. Persistir Payment
     * 5. Atualizar assinatura vinculada (apenas se status = APPROVED)
     * 
     * Regras de negócio:
     * - NÃO ativar assinatura se status != APPROVED
     * - NÃO confiar apenas em metadata
     * - Se metadata ausente → buscar assinatura pelo payment_id
     * 
     * @param paymentData Dados do pagamento do webhook (não confiar apenas nisso)
     */
    @Transactional
    private void handleMercadoPagoPayment(Map<String, Object> paymentData) {
        log.info("=== Iniciando processamento de PAYMENT ===");

        try {
            // 1. Extrair ID do pagamento do webhook
            String paymentIdStr = String.valueOf(paymentData.get("id"));
            Long paymentId;

            try {
                paymentId = Long.parseLong(paymentIdStr);
            } catch (NumberFormatException e) {
                log.error("ID de pagamento inválido: {}", paymentIdStr);
                return;
            }

            log.info("Payment ID do webhook: {}", paymentId);

            // 2. Buscar pagamento na API Mercado Pago
            // GET https://api.mercadopago.com/v1/payments/{id}
            // Authorization: Bearer ACCESS_TOKEN
            com.mercadopago.resources.payment.Payment mpPayment;
            try {
                mpPayment = mercadoPagoService.getPayment(paymentId);
                log.info("✓ Detalhes do pagamento {} obtidos via API do Mercado Pago", paymentId);
            } catch (MPException | MPApiException e) {
                log.error("✗ Erro ao buscar detalhes do pagamento {} via API: {}", paymentId, e.getMessage(), e);
                return;
            }

            // 3. Verificar idempotência e ordem temporal usando mercado_pago_payment_id
            // Garantir que não processamos o mesmo evento duas vezes E não regredimos
            // status
            Optional<Payment> existingPaymentOpt = paymentRepository.findByMercadoPagoPaymentId(paymentId);

            // Extrair timestamp do evento (fonte da verdade: API do Mercado Pago)
            LocalDateTime eventTimestamp = extractPaymentTimestamp(mpPayment);

            if (existingPaymentOpt.isPresent()) {
                Payment existingPayment = existingPaymentOpt.get();
                log.info("Pagamento {} já foi processado anteriormente (ID local: {}), verificando ordem temporal...",
                        paymentId, existingPayment.getId());

                // VALIDAÇÃO DE ORDEM TEMPORAL: Prevenir regressão de status
                // Se o evento recebido é mais antigo que o último status processado, ignorar
                if (existingPayment.getLastStatusUpdateAt() != null &&
                        eventTimestamp.isBefore(existingPayment.getLastStatusUpdateAt())) {

                    String currentStatus = existingPayment.getStatus().toString();
                    String receivedStatus = mpPayment.getStatus() != null ? mpPayment.getStatus().toString()
                            : "unknown";

                    log.warn("⚠ Evento fora de ordem ignorado - paymentId={}, statusAtual={}, statusRecebido={}, " +
                            "timestampAtual={}, timestampRecebido={}",
                            paymentId, currentStatus, receivedStatus,
                            existingPayment.getLastStatusUpdateAt(), eventTimestamp);

                    log.warn(
                            "⚠ REGRA: Não permitir regressão de status. Status atual ({}) é mais recente que o recebido ({}).",
                            currentStatus, receivedStatus);

                    return; // IGNORA evento mais antigo - previne regressão de status
                }

                // Se já foi processado e está aprovado com mesmo status, não processar
                // novamente (idempotência)
                if (existingPayment.getStatus() == Payment.PaymentStatus.APPROVED &&
                        "approved".equalsIgnoreCase(
                                mpPayment.getStatus() != null ? mpPayment.getStatus().toString() : null)) {
                    log.info("Pagamento {} já está aprovado e processado, ignorando reprocessamento", paymentId);
                    return;
                }
            }

            // 4. Validar status real do pagamento na API
            // NÃO confiar apenas no webhook, sempre buscar status atualizado
            String status = mpPayment.getStatus() != null ? mpPayment.getStatus().toString() : null;
            String statusDetail = null;
            try {
                statusDetail = mpPayment.getStatusDetail();
            } catch (Exception e) {
                log.debug("StatusDetail não disponível: {}", e.getMessage());
            }

            // Validar status: approved, pending, cancelled são os únicos válidos para
            // processamento
            if (status == null || status.isEmpty()) {
                log.error("Status do pagamento não pode ser nulo ou vazio para payment_id: {}", paymentId);
                throw new WebhookProcessingException(
                        "Status do pagamento inválido: nulo ou vazio",
                        String.valueOf(paymentId),
                        "payment",
                        null);
            }

            log.info("Status do pagamento validado via API: {} ({})", status, statusDetail);

            // Mapear status conforme obrigatório
            Payment.PaymentStatus paymentStatus = mapMercadoPagoStatusToPaymentStatus(status);
            log.info("Status mapeado para PaymentStatus: {}", paymentStatus);

            // Validação adicional: garantir que status é válido
            if (paymentStatus == null) {
                log.error("Status mapeado é nulo para status: {}", status);
                throw new WebhookProcessingException(
                        "Status do pagamento não pôde ser mapeado: " + status,
                        String.valueOf(paymentId),
                        "payment",
                        null);
            }

            // Extrair valores e informações do pagamento
            BigDecimal transactionAmount = extractTransactionAmount(mpPayment);
            String currencyId = extractCurrencyId(mpPayment);
            String paymentMethodId = extractPaymentMethodId(mpPayment);

            // 5. Buscar assinatura vinculada
            // Lógica: Primeiro tentar via metadata.subscription_id, depois API, registrar
            // erro se não encontrar
            Subscription subscription = findSubscriptionForPayment(mpPayment, paymentId);

            if (subscription == null) {
                log.error("✗ ERRO CRÍTICO: Assinatura não encontrada para o pagamento {}. " +
                        "Payment NÃO será criado sem assinatura (subscription é obrigatória).", paymentId);
                // Payment tem constraint NOT NULL em subscription_id, então não podemos criar
                // sem assinatura
                throw new WebhookProcessingException(
                        "Assinatura não encontrada para o pagamento " + paymentId + ". " +
                                "Payment requer uma assinatura vinculada.",
                        String.valueOf(paymentId),
                        "payment",
                        null);
            }

            // 6. Persistir Payment (com validação de ordem temporal já feita acima)
            Payment payment = createOrUpdatePayment(
                    existingPaymentOpt,
                    subscription,
                    paymentId,
                    paymentStatus,
                    transactionAmount,
                    currencyId,
                    paymentMethodId,
                    status,
                    statusDetail,
                    mpPayment,
                    eventTimestamp);

            paymentRepository.save(payment);
            log.info("✓ Payment persistido: ID={}, MercadoPagoPaymentId={}, Status={}",
                    payment.getId(), payment.getMercadoPagoPaymentId(), payment.getStatus());

            // 7. Atualizar assinatura vinculada e liberar produto (apenas se status =
            // APPROVED)
            // Regra de negócio: NÃO liberar produto se status != APPROVED
            if ("approved".equalsIgnoreCase(status) && subscription != null) {
                log.info("✓ Pagamento aprovado - ativando assinatura e liberando acesso ao produto");
                updateSubscriptionForApprovedPayment(subscription, payment);
                // Produto é liberado automaticamente quando assinatura é ativada
                try {
                    BigDecimal amountForAlert = transactionAmount != null ? transactionAmount : BigDecimal.ZERO;
                    alertService.sendPaymentApprovedAlert(
                            String.valueOf(payment.getMercadoPagoPaymentId()),
                            String.valueOf(subscription.getUser().getId()),
                            amountForAlert);
                } catch (Exception ignored) {
                }
            } else if (!"approved".equalsIgnoreCase(status) && subscription != null) {
                log.warn("✗ Pagamento com status {} - assinatura NÃO será ativada e produto NÃO será liberado " +
                        "(regra: apenas APPROVED libera acesso)", status);

                // Processar outros status (rejected, cancelled, etc.)
                processPaymentStatusActions(payment, subscription, status, statusDetail);
            } else if (subscription == null) {
                log.warn("Assinatura não encontrada - não é possível ativar mesmo com status APPROVED");
            }

            log.info("=== Processamento de PAYMENT concluído ===");
        } catch (Exception e) {
            log.error("Erro ao processar pagamento do Mercado Pago: {}", e.getMessage(), e);
        }
    }

    /**
     * Busca assinatura vinculada ao pagamento
     * 
     * Lógica de busca (em ordem de prioridade):
     * 1. Primeiro tentar via metadata.subscription_id
     * 2. Se não existir, buscar assinatura via API Mercado Pago
     * 3. Registrar erro se não encontrar vínculo
     * 
     * @param mpPayment Pagamento do Mercado Pago
     * @param paymentId ID do pagamento
     * @return Subscription encontrada ou null (com erro registrado)
     */
    private Subscription findSubscriptionForPayment(
            com.mercadopago.resources.payment.Payment mpPayment,
            Long paymentId) {

        log.info("Buscando assinatura vinculada ao pagamento {}", paymentId);

        // 1. Primeiro tentar via metadata.subscription_id
        Map<String, Object> metadata = null;
        try {
            metadata = mpPayment.getMetadata();
        } catch (Exception e) {
            log.debug("Metadata não disponível: {}", e.getMessage());
        }

        String subscriptionId = null;
        String preferenceId = null;
        String userId = null;

        if (metadata != null) {
            // Prioridade 1: metadata.subscription_id
            subscriptionId = (String) metadata.get("subscription_id");
            preferenceId = (String) metadata.get("preference_id");
            userId = (String) metadata.get("user_id");

            log.debug("Metadata encontrado - subscription_id: {}, preference_id: {}, user_id: {}",
                    subscriptionId, preferenceId, userId);
        }

        // Buscar por subscription_id (prioridade mais alta)
        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            try {
                Long subscriptionIdLong = Long.parseLong(subscriptionId);
                Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionIdLong);
                if (subscriptionOpt.isPresent()) {
                    log.info("✓ Assinatura encontrada via metadata.subscription_id: {}", subscriptionId);
                    return subscriptionOpt.get();
                } else {
                    log.warn("Assinatura não encontrada no banco para subscription_id: {}", subscriptionId);
                }
            } catch (NumberFormatException e) {
                log.warn("subscription_id inválido no metadata: {}", subscriptionId);
            }
        }

        // Buscar por preference_id (se disponível)
        if (preferenceId != null && !preferenceId.isEmpty()) {
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                    .findByMercadoPagoSubscriptionId(preferenceId);
            if (subscriptionOpt.isPresent()) {
                log.info("✓ Assinatura encontrada por preference_id (mp_sub_id): {}", preferenceId);
                return subscriptionOpt.get();
            } else {
                log.warn("Assinatura não encontrada para preference_id: {}", preferenceId);
            }
        }

        // Buscar por user_id (se disponível)
        if (userId != null && !userId.isEmpty()) {
            try {
                Long userIdLong = Long.parseLong(userId);
                Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userIdLong);
                if (subscriptionOpt.isPresent()) {
                    log.info("✓ Assinatura encontrada por user_id: {}", userId);
                    return subscriptionOpt.get();
                }
            } catch (NumberFormatException e) {
                log.warn("user_id inválido: {}", userId);
            }
        }

        // 2. Se não encontrou via metadata, buscar assinatura via API Mercado Pago
        try {
            log.info("Tentando buscar assinatura via API Mercado Pago para pagamento {}", paymentId);
            // Buscar informações adicionais do pagamento via API
            // O pagamento pode ter referência a uma preference/preapproval
            String orderId = null;
            try {
                Object orderIdObj = mpPayment.getOrder();
                if (orderIdObj != null) {
                    orderId = orderIdObj.toString();
                }
            } catch (Exception e) {
                log.debug("Order ID não disponível: {}", e.getMessage());
            }

            // Se tiver order_id, pode buscar merchant_order e encontrar a assinatura
            if (orderId != null) {
                log.debug("Order ID encontrado: {}, mas busca de merchant_order não implementada ainda", orderId);
                // TODO: Implementar busca de merchant_order via API se necessário
            }
        } catch (Exception e) {
            log.debug("Erro ao buscar assinatura via API: {}", e.getMessage());
        }

        // 3. Última tentativa: buscar Payment existente e pegar a assinatura dele
        Optional<Payment> existingPayment = paymentRepository.findByMercadoPagoPaymentId(paymentId);
        if (existingPayment.isPresent() && existingPayment.get().getSubscription() != null) {
            log.info("✓ Assinatura encontrada via payment_id existente: {}", paymentId);
            return existingPayment.get().getSubscription();
        }

        // 4. Registrar erro se não encontrar vínculo
        log.error("✗ ERRO: Nenhuma assinatura encontrada para o pagamento {}. " +
                "Tentativas: metadata.subscription_id={}, preference_id={}, user_id={}, payment_id existente",
                paymentId, subscriptionId, preferenceId, userId);

        return null;
    }

    /**
     * Extrai valor da transação do pagamento
     */
    private BigDecimal extractTransactionAmount(com.mercadopago.resources.payment.Payment mpPayment) {
        try {
            Object amountObj = mpPayment.getTransactionAmount();
            if (amountObj != null) {
                if (amountObj instanceof Double) {
                    return BigDecimal.valueOf((Double) amountObj);
                } else if (amountObj instanceof BigDecimal) {
                    return (BigDecimal) amountObj;
                } else if (amountObj instanceof Number) {
                    return BigDecimal.valueOf(((Number) amountObj).doubleValue());
                }
            }
        } catch (Exception e) {
            log.debug("TransactionAmount não disponível: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extrai moeda do pagamento
     */
    private String extractCurrencyId(com.mercadopago.resources.payment.Payment mpPayment) {
        try {
            String currencyId = mpPayment.getCurrencyId();
            return currencyId != null ? currencyId : "BRL";
        } catch (Exception e) {
            log.debug("CurrencyId não disponível, usando BRL: {}", e.getMessage());
            return "BRL";
        }
    }

    /**
     * Extrai método de pagamento
     */
    private String extractPaymentMethodId(com.mercadopago.resources.payment.Payment mpPayment) {
        try {
            return mpPayment.getPaymentMethodId();
        } catch (Exception e) {
            log.debug("PaymentMethodId não disponível: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai timestamp confiável do pagamento usando a API do Mercado Pago
     * 
     * Fonte da verdade: date_last_updated da API (não confiar apenas no webhook)
     * Usado para validar ordem temporal e prevenir regressão de status
     * 
     * @param mpPayment Pagamento obtido da API do Mercado Pago
     * @return LocalDateTime com timestamp do evento, ou LocalDateTime.now() se não
     *         disponível
     */
    private LocalDateTime extractPaymentTimestamp(com.mercadopago.resources.payment.Payment mpPayment) {
        try {
            // Prioridade 1: date_last_updated (mais confiável - indica última mudança de
            // status)
            Object dateLastUpdatedObj = mpPayment.getDateLastUpdated();
            if (dateLastUpdatedObj != null) {
                LocalDateTime timestamp = convertToLocalDateTime(dateLastUpdatedObj);
                if (timestamp != null) {
                    log.debug("Timestamp extraído de date_last_updated: {}", timestamp);
                    return timestamp;
                }
            }

            // Prioridade 2: date_approved (se status é approved)
            if ("approved".equalsIgnoreCase(mpPayment.getStatus() != null ? mpPayment.getStatus().toString() : null)) {
                Object dateApprovedObj = mpPayment.getDateApproved();
                if (dateApprovedObj != null) {
                    LocalDateTime timestamp = convertToLocalDateTime(dateApprovedObj);
                    if (timestamp != null) {
                        log.debug("Timestamp extraído de date_approved: {}", timestamp);
                        return timestamp;
                    }
                }
            }

            // Fallback: usar data de criação
            Object dateCreatedObj = mpPayment.getDateCreated();
            if (dateCreatedObj != null) {
                LocalDateTime timestamp = convertToLocalDateTime(dateCreatedObj);
                if (timestamp != null) {
                    log.debug("Timestamp extraído de date_created: {}", timestamp);
                    return timestamp;
                }
            }

            // Último fallback: usar agora (não ideal, mas melhor que null)
            log.warn("Não foi possível extrair timestamp do pagamento, usando LocalDateTime.now()");
            return LocalDateTime.now();

        } catch (Exception e) {
            log.warn("Erro ao extrair timestamp do pagamento: {}, usando LocalDateTime.now()", e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * Cria ou atualiza Payment com os dados do Mercado Pago
     * 
     * IMPORTANTE: Validação de ordem temporal já foi feita antes de chamar este
     * método
     * Este método apenas atualiza o payment com o status mais recente
     * 
     * @param eventTimestamp Timestamp do evento (fonte da verdade: API do Mercado
     *                       Pago)
     */
    private Payment createOrUpdatePayment(
            Optional<Payment> existingPaymentOpt,
            Subscription subscription,
            Long paymentId,
            Payment.PaymentStatus paymentStatus,
            BigDecimal transactionAmount,
            String currencyId,
            String paymentMethodId,
            String status,
            String statusDetail,
            com.mercadopago.resources.payment.Payment mpPayment,
            LocalDateTime eventTimestamp) {

        // Criar ou atualizar Payment
        final Subscription finalSubscription = subscription;
        Payment payment = existingPaymentOpt.orElseGet(() -> {
            Payment newPayment = new Payment();
            if (finalSubscription != null) {
                newPayment.setSubscription(finalSubscription);
            }
            return newPayment;
        });

        // Atualizar campos do Payment
        payment.setMercadoPagoPaymentId(paymentId);
        payment.setStatus(paymentStatus);

        // IMPORTANTE: Atualizar timestamp da última mudança de status
        // Isso garante que eventos futuros possam validar ordem temporal
        payment.setLastStatusUpdateAt(eventTimestamp);
        log.debug("Atualizado lastStatusUpdateAt para payment {}: {}", paymentId, eventTimestamp);

        if (transactionAmount != null) {
            payment.setAmount(transactionAmount);
        }

        payment.setCurrency(currencyId);

        if (paymentMethodId != null) {
            payment.setPaymentMethod(paymentMethodId);
        }

        // Datas baseadas no status
        LocalDateTime now = LocalDateTime.now();
        if ("approved".equalsIgnoreCase(status)) {
            try {
                Object dateApprovedObj = mpPayment.getDateApproved();
                if (dateApprovedObj != null) {
                    LocalDateTime dateApproved = convertToLocalDateTime(dateApprovedObj);
                    payment.setPaidAt(dateApproved != null ? dateApproved : now);
                } else {
                    payment.setPaidAt(now);
                }
            } catch (Exception e) {
                log.debug("DateApproved não disponível, usando agora: {}", e.getMessage());
                payment.setPaidAt(now);
            }
        } else if ("rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)
                || "canceled".equalsIgnoreCase(status)) {
            try {
                Object dateLastUpdatedObj = mpPayment.getDateLastUpdated();
                if (dateLastUpdatedObj != null) {
                    LocalDateTime dateLastUpdated = convertToLocalDateTime(dateLastUpdatedObj);
                    payment.setFailedAt(dateLastUpdated != null ? dateLastUpdated : now);
                } else {
                    payment.setFailedAt(now);
                }
            } catch (Exception e) {
                log.debug("DateLastUpdated não disponível, usando agora: {}", e.getMessage());
                payment.setFailedAt(now);
            }
            payment.setFailureReason(statusDetail);
        }

        return payment;
    }

    /**
     * Atualiza assinatura vinculada quando pagamento é aprovado
     * 
     * Regra: NÃO ativar assinatura se status != APPROVED
     * Este método só é chamado quando status = APPROVED
     */
    private void updateSubscriptionForApprovedPayment(Subscription subscription, Payment payment) {
        try {
            subscriptionService.activateOrRenewSubscription(
                    subscription.getUser().getId(),
                    subscription.getPlan() != null ? subscription.getPlan().getId() : null,
                    payment.getMercadoPagoPaymentId());
            log.info("✓ Assinatura ativada/renovada via pagamento aprovado: {}", payment.getMercadoPagoPaymentId());
        } catch (Exception e) {
            log.error("Erro ao ativar assinatura via pagamento aprovado: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa lógica de negócio baseada no status do pagamento
     * 
     * @deprecated Este método não deve mais ser usado diretamente.
     *             A lógica de atualização de assinatura agora é feita em
     *             updateSubscriptionForApprovedPayment
     */
    @Deprecated
    private void processPaymentBusinessLogic(Long paymentId, String status, String preferenceId,
            String userId, String planId, Map<String, Object> paymentData,
            String statusDetail) {
        switch (status != null ? status.toLowerCase() : "") {
            case "approved" -> {
                log.info("✓ Pagamento aprovado: {}", paymentId);
                handlePaymentApproved(String.valueOf(paymentId), preferenceId, userId, planId, paymentData);
            }
            case "rejected" -> {
                log.warn("✗ Pagamento rejeitado: {} - {}", paymentId, statusDetail);
                handlePaymentRejected(String.valueOf(paymentId), preferenceId, userId, statusDetail);
            }
            case "pending", "in_process" -> {
                log.info("⏳ Pagamento pendente: {} - {}", paymentId, statusDetail);
                handlePaymentPending(String.valueOf(paymentId), preferenceId, userId, statusDetail);
            }
            case "cancelled", "canceled" -> {
                log.warn("✗ Pagamento cancelado: {}", paymentId);
                handlePaymentCancelled(String.valueOf(paymentId), preferenceId, userId);
            }
            case "refunded" -> {
                log.warn("↩ Pagamento reembolsado: {}", paymentId);
                handlePaymentRefunded(String.valueOf(paymentId), preferenceId, userId);
            }
            case "charged_back" -> {
                log.warn("⚠ Pagamento estornado: {}", paymentId);
                // Buscar subscription para passar ao handler
                Optional<Payment> paymentOpt = paymentRepository.findByMercadoPagoPaymentId(paymentId);
                Subscription subscription = paymentOpt.map(Payment::getSubscription).orElse(null);
                handlePaymentChargedBack(String.valueOf(paymentId), preferenceId, userId, subscription);
            }
            default -> {
                log.warn("? Status de pagamento desconhecido: {} - {}", status, paymentId);
            }
        }
    }

    /**
     * Processa pagamento aprovado
     * 
     * @deprecated A lógica de ativação de assinatura agora é feita em
     *             updateSubscriptionForApprovedPayment
     *             Este método é mantido apenas para compatibilidade com outros
     *             fluxos
     */
    @Deprecated
    private void handlePaymentApproved(String paymentId, String preferenceId, String userId,
            String planId, Map<String, Object> paymentData) {
        try {
            if (userId != null && planId != null) {
                // Criar nova assinatura se não existir
                try {
                    Long userIdLong = Long.parseLong(userId);
                    Long planIdLong = Long.parseLong(planId);

                    subscriptionService.activateOrRenewSubscription(
                            userIdLong, planIdLong, Long.parseLong(paymentId));
                    log.info("Assinatura ativada/renovada via pagamento aprovado para usuário {}", userIdLong);
                } catch (NumberFormatException e) {
                    log.error("Erro ao converter IDs: userId={}, planId={}", userId, planId);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento aprovado: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa pagamento rejeitado
     */
    private void handlePaymentRejected(String paymentId, String preferenceId, String userId, String statusDetail) {
        try {
            if (userId != null) {
                subscriptionService.updateSubscriptionStatusByUserId(
                        Long.parseLong(userId), SubscriptionStatus.PAST_DUE);
                log.warn("Assinatura marcada como past_due devido a pagamento rejeitado para usuário {} - {}",
                        userId, statusDetail);
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento rejeitado: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa pagamento pendente
     */
    private void handlePaymentPending(String paymentId, String preferenceId, String userId, String statusDetail) {
        try {
            if (userId != null) {
                subscriptionService.updateSubscriptionStatusByUserId(
                        Long.parseLong(userId), SubscriptionStatus.INCOMPLETE);
                log.info("Assinatura mantida como incomplete - aguardando confirmação de pagamento: {}",
                        statusDetail);
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento pendente: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa pagamento cancelado
     */
    private void handlePaymentCancelled(String paymentId, String preferenceId, String userId) {
        try {
            if (userId != null) {
                subscriptionService.updateSubscriptionStatusByUserId(
                        Long.parseLong(userId), SubscriptionStatus.CANCELED);
                log.warn("Assinatura cancelada devido a pagamento cancelado para usuário {}", userId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento cancelado: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa pagamento reembolsado
     */
    private void handlePaymentRefunded(String paymentId, String preferenceId, String userId) {
        try {
            if (userId != null) {
                subscriptionService.updateSubscriptionStatusByUserId(
                        Long.parseLong(userId), SubscriptionStatus.CANCELED);
                log.warn("Assinatura cancelada devido a reembolso para usuário {}", userId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento reembolsado: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa ações baseadas no status do pagamento
     */
    private void processPaymentStatusActions(Payment payment, Subscription subscription,
            String status, String statusDetail) {
        switch (status != null ? status.toLowerCase() : "") {
            case "rejected" -> {
                log.warn("Pagamento rejeitado: {} - {}", payment.getMercadoPagoPaymentId(), statusDetail);
                handlePaymentRejected(
                        String.valueOf(payment.getMercadoPagoPaymentId()),
                        null,
                        String.valueOf(subscription.getUser().getId()),
                        statusDetail);
                try {
                    alertService.sendPaymentRejectedAlert(
                            String.valueOf(payment.getMercadoPagoPaymentId()),
                            String.valueOf(subscription.getUser().getId()),
                            statusDetail != null ? statusDetail : "unknown");
                } catch (Exception ignored) {
                }
            }
            case "pending", "in_process" -> {
                log.info("Pagamento pendente: {} - {}", payment.getMercadoPagoPaymentId(), statusDetail);
                handlePaymentPending(
                        String.valueOf(payment.getMercadoPagoPaymentId()),
                        null,
                        String.valueOf(subscription.getUser().getId()),
                        statusDetail);
            }
            case "cancelled", "canceled" -> {
                log.warn("Pagamento cancelado: {}", payment.getMercadoPagoPaymentId());
                handlePaymentCancelled(
                        String.valueOf(payment.getMercadoPagoPaymentId()),
                        null,
                        String.valueOf(subscription.getUser().getId()));
                try {
                    alertService.sendPaymentRejectedAlert(
                            String.valueOf(payment.getMercadoPagoPaymentId()),
                            String.valueOf(subscription.getUser().getId()),
                            "cancelled");
                } catch (Exception ignored) {
                }
            }
            case "refunded" -> {
                log.warn("Pagamento reembolsado: {}", payment.getMercadoPagoPaymentId());
                handlePaymentRefunded(
                        String.valueOf(payment.getMercadoPagoPaymentId()),
                        null,
                        String.valueOf(subscription.getUser().getId()));
            }
            case "charged_back" -> {
                log.error("⚠ CHARGEBACK detectado: {}", payment.getMercadoPagoPaymentId());
                handlePaymentChargedBack(
                        String.valueOf(payment.getMercadoPagoPaymentId()),
                        null,
                        String.valueOf(subscription.getUser().getId()),
                        subscription);
                try {
                    alertService.sendChargebackAlert(
                            String.valueOf(payment.getMercadoPagoPaymentId()),
                            String.valueOf(subscription.getUser().getId()));
                    monitoringService.recordChargebackDetected(
                            String.valueOf(payment.getMercadoPagoPaymentId()),
                            String.valueOf(subscription.getUser().getId()));
                } catch (Exception ignored) {
                }
            }
            default -> {
                log.warn("Status de pagamento desconhecido: {} - {}", status, payment.getMercadoPagoPaymentId());
            }
        }
    }

    /**
     * Processa pagamento estornado (chargeback)
     * 
     * Ações obrigatórias:
     * - Atualizar Payment.status = CHARGED_BACK
     * - Suspender Subscription (PAST_DUE)
     * - Registrar histórico
     * - NÃO permitir reativação automática
     */
    private void handlePaymentChargedBack(String paymentId, String preferenceId, String userId,
            Subscription subscription) {
        try {
            log.error("=== PROCESSANDO CHARGEBACK ===");
            log.error("Payment ID: {}", paymentId);
            log.error("Subscription ID: {}", subscription != null ? subscription.getId() : "null");

            // 1. Atualizar Payment.status = CHARGED_BACK
            Optional<Payment> paymentOpt = paymentRepository.findByMercadoPagoPaymentId(Long.parseLong(paymentId));
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(Payment.PaymentStatus.CHARGED_BACK);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason("Chargeback detectado - acesso bloqueado");
                paymentRepository.save(payment);
                log.error("✓ Payment atualizado para CHARGED_BACK: {}", payment.getId());
            } else {
                log.error("✗ Payment não encontrado para chargeback: {}", paymentId);
            }

            // 2. Suspender Subscription
            if (subscription != null) {
                subscription.setStatus(SubscriptionStatus.PAST_DUE);
                subscription.setEndedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                log.error("✓ Subscription suspensa (PAST_DUE) devido a chargeback: {}", subscription.getId());

                // 3. Registrar histórico (log detalhado)
                log.error("=== HISTÓRICO DE CHARGEBACK ===");
                log.error("Data/Hora: {}", LocalDateTime.now());
                log.error("Payment ID: {}", paymentId);
                log.error("Subscription ID: {}", subscription.getId());
                log.error("User ID: {}", subscription.getUser() != null ? subscription.getUser().getId() : "null");
                log.error("Status anterior: {}", subscription.getStatus());
                log.error("Ação: Subscription suspensa - acesso bloqueado");
                log.error("Reativação automática: NÃO PERMITIDA");
                log.error("=================================");
            } else {
                log.error("✗ Subscription não encontrada para chargeback: preference_id={}", preferenceId);
            }

            // 4. Garantir que não há reativação automática
            // A assinatura está em PAST_DUE e só pode ser reativada manualmente
            log.error("⚠ IMPORTANTE: Reativação automática está DESABILITADA para esta assinatura");
            log.error("A assinatura requer intervenção manual para reativação após chargeback");

        } catch (Exception e) {
            log.error("✗ ERRO CRÍTICO ao processar chargeback: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar chargeback: " + e.getMessage(), e);
        }
    }

    /**
     * Processa evento subscription_preapproval (webhook do Mercado Pago).
     * <ul>
     *   <li>Identifica o usuário pelo <b>external_reference</b> (ID do usuário na aplicação).</li>
     *   <li><b>authorized / approved</b> → libera o usuário (assinatura ACTIVE, acesso liberado).</li>
     *   <li><b>cancelled</b> → bloqueia o usuário (assinatura CANCELED, API deve negar acesso).</li>
     * </ul>
     * Atualiza status da assinatura e vincula ao usuário/plano.
     *
     * @param preapprovalData Dados da assinatura do Mercado Pago (inclui external_reference, status, etc.)
     * @param action         Ação do evento (created, updated, cancelled, etc.)
     */
    @Transactional
    public void handleSubscriptionPreapproval(Map<String, Object> preapprovalData, String action) {
        log.info("=== Processando subscription_preapproval (PreapprovalPlan) ===");
        log.info("Action: {}", action);

        try {
            // CORRETO: Agora é preapproval_id (não preference_id)
            String preapprovalId = (String) preapprovalData.get("id");
            if (preapprovalId == null || preapprovalId.isEmpty()) {
                log.error("Preapproval ID não encontrado no payload");
                throw new WebhookProcessingException("Preapproval ID não encontrado", null, "subscription_preapproval",
                        null);
            }

            log.info("Preapproval ID: {}", preapprovalId);

            // Buscar assinatura existente ou criar nova
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                    .findByMercadoPagoSubscriptionId(preapprovalId);
            Subscription subscription;

            if (subscriptionOpt.isPresent()) {
                subscription = subscriptionOpt.get();
                log.info("✓ Assinatura existente encontrada: ID={}, PreapprovalId={}", subscription.getId(),
                        preapprovalId);
            } else {
                log.info("Criando nova assinatura para preapproval_id: {}", preapprovalId);
                subscription = createSubscriptionFromPreapproval(preapprovalData, preapprovalId);
            }

            // Atualizar status baseado na ação e dados do preapproval
            updateSubscriptionStatusFromPreapproval(subscription, preapprovalData, action);

            // Garantir renovação mensal correta
            updateSubscriptionRenewalPeriod(subscription);

            subscriptionRepository.save(subscription);
            log.info("✓ Assinatura atualizada: ID={}, Status={}, PreapprovalId={}",
                    subscription.getId(), subscription.getStatus(), preapprovalId);

            log.info("=== Processamento de subscription_preapproval concluído ===");

        } catch (Exception e) {
            log.error("Erro ao processar subscription_preapproval: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar subscription_preapproval: " + e.getMessage(), e);
        }
    }

    /**
     * Processa evento subscription_authorized_payment (renovação mensal)
     * 
     * Trata pagamentos autorizados para renovação:
     * - Vincula pagamento à assinatura
     * - Atualiza período de renovação
     * - Garante continuidade da assinatura
     * 
     * @param subscriptionId ID da assinatura
     * @param paymentData    Dados do pagamento autorizado
     */
    @Transactional
    public void handleSubscriptionAuthorizedPayment(String subscriptionId, Map<String, Object> paymentData) {
        log.info("=== Processando subscription_authorized_payment (renovação mensal) ===");
        log.info("Subscription ID: {}", subscriptionId);

        try {
            // Buscar assinatura
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                    .findByMercadoPagoSubscriptionId(subscriptionId);

            if (subscriptionOpt.isEmpty()) {
                log.error("Assinatura não encontrada para subscription_id: {}", subscriptionId);
                throw new WebhookProcessingException("Assinatura não encontrada: " + subscriptionId,
                        subscriptionId, "subscription_authorized_payment", null);
            }

            Subscription subscription = subscriptionOpt.get();

            // Extrair payment_id do pagamento autorizado
            String paymentIdStr = String.valueOf(paymentData.get("id"));
            Long paymentId;
            try {
                paymentId = Long.parseLong(paymentIdStr);
            } catch (NumberFormatException e) {
                log.error("Payment ID inválido: {}", paymentIdStr);
                throw new WebhookProcessingException("Payment ID inválido: " + paymentIdStr,
                        subscriptionId, "subscription_authorized_payment", null);
            }

            // Buscar pagamento completo via API
            com.mercadopago.resources.payment.Payment mpPayment;
            try {
                mpPayment = mercadoPagoService.getPayment(paymentId);
                log.info("✓ Pagamento de renovação {} obtido via API", paymentId);
            } catch (MPException | MPApiException e) {
                log.error("Erro ao buscar pagamento de renovação {} via API: {}", paymentId, e.getMessage(), e);
                throw new WebhookProcessingException("Erro ao buscar pagamento: " + e.getMessage(),
                        subscriptionId, "subscription_authorized_payment", e);
            }

            // Verificar se payment já existe
            Optional<Payment> existingPaymentOpt = paymentRepository.findByMercadoPagoPaymentId(paymentId);
            Payment payment;

            if (existingPaymentOpt.isPresent()) {
                payment = existingPaymentOpt.get();
                log.info("Payment de renovação já existe: {}", payment.getId());
            } else {
                // Criar novo payment vinculado à assinatura
                payment = new Payment();
                payment.setSubscription(subscription);
                payment.setMercadoPagoPaymentId(paymentId);

                // Extrair dados do pagamento
                String status = mpPayment.getStatus() != null ? mpPayment.getStatus().toString() : null;
                payment.setStatus(mapMercadoPagoStatusToPaymentStatus(status));

                BigDecimal transactionAmount = extractTransactionAmount(mpPayment);
                if (transactionAmount != null) {
                    payment.setAmount(transactionAmount);
                }

                payment.setCurrency(extractCurrencyId(mpPayment));
                payment.setPaymentMethod(extractPaymentMethodId(mpPayment));

                if ("approved".equalsIgnoreCase(status)) {
                    payment.setPaidAt(LocalDateTime.now());
                }

                paymentRepository.save(payment);
                log.info("✓ Payment de renovação criado: {}", payment.getId());
            }

            // Atualizar assinatura para renovação mensal
            if ("approved".equalsIgnoreCase(mpPayment.getStatus() != null ? mpPayment.getStatus().toString() : null)) {
                LocalDateTime now = LocalDateTime.now();
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setCurrentPeriodStart(now);
                subscription.setCurrentPeriodEnd(now.plusMonths(1)); // Renovação mensal
                subscriptionRepository.save(subscription);
                log.info("✓ Assinatura renovada mensalmente: novo período até {}", subscription.getCurrentPeriodEnd());
            } else {
                log.warn("Pagamento de renovação não está aprovado - assinatura não será renovada");
            }

            log.info("=== Processamento de subscription_authorized_payment concluído ===");

        } catch (Exception e) {
            log.error("Erro ao processar subscription_authorized_payment: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar subscription_authorized_payment: " + e.getMessage(), e);
        }
    }

    /**
     * Cria assinatura a partir de dados de preapproval (PreapprovalPlan).
     * Identifica o usuário pelo external_reference (ID do usuário logado enviado na criação da assinatura).
     * Plano: metadata.plan_id (ID interno) ou preapproval_plan_id (ID do plano no Mercado Pago).
     */
    private Subscription createSubscriptionFromPreapproval(Map<String, Object> preapprovalData, String preapprovalId) {
        // 1) Usuário: external_reference = ID do usuário na aplicação (obrigatório para comparar com usuário logado)
        String externalRef = preapprovalData.get("external_reference") != null
                ? String.valueOf(preapprovalData.get("external_reference"))
                : null;
        Map<String, Object> metadata = null;
        try {
            Object metadataObj = preapprovalData.get("metadata");
            if (metadataObj instanceof Map) {
                metadata = (Map<String, Object>) metadataObj;
            }
        } catch (Exception e) {
            log.debug("Metadata não disponível: {}", e.getMessage());
        }

        String userIdStr = null;
        if (metadata != null && metadata.get("user_id") != null) {
            userIdStr = String.valueOf(metadata.get("user_id"));
        }
        if (userIdStr == null || userIdStr.isBlank()) {
            userIdStr = externalRef;
        }
        if (userIdStr == null || userIdStr.isBlank()) {
            log.error("external_reference (ID do usuário) não encontrado no preapproval");
            throw new WebhookProcessingException("external_reference (ID do usuário) não encontrado",
                    preapprovalId, "subscription_preapproval", null);
        }

        Long userId = Long.parseLong(userIdStr.trim());

        // 2) Plano: preapproval_plan_id = ID do plano no Mercado Pago (plano já criado no painel)
        String mpPlanId = preapprovalData.get("preapproval_plan_id") != null
                ? String.valueOf(preapprovalData.get("preapproval_plan_id")).trim()
                : null;
        if (mpPlanId != null && mpPlanId.isEmpty()) {
            mpPlanId = null;
        }
        final String mpPlanIdFinal = mpPlanId;

        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new WebhookProcessingException("Usuário não encontrado: " + userId,
                        preapprovalId, "subscription_preapproval", null));

        Plan plan;
        if (mpPlanIdFinal != null && !mpPlanIdFinal.isBlank()) {
            plan = planRepository.findByMercadoPagoPreapprovalPlanId(mpPlanIdFinal)
                    .orElseThrow(() -> new WebhookProcessingException("Plano não encontrado para preapproval_plan_id: " + mpPlanIdFinal,
                            preapprovalId, "subscription_preapproval", null));
        } else if (metadata != null && metadata.get("plan_id") != null) {
            Long planId = Long.parseLong(String.valueOf(metadata.get("plan_id")));
            plan = planRepository.findById(planId)
                    .orElseThrow(() -> new WebhookProcessingException("Plano não encontrado: " + planId,
                            preapprovalId, "subscription_preapproval", null));
        } else {
            log.error("preapproval_plan_id ou metadata.plan_id não encontrados");
            throw new WebhookProcessingException("Plano não identificado (preapproval_plan_id ou metadata.plan_id)",
                    preapprovalId, "subscription_preapproval", null);
        }

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setMercadoPagoSubscriptionId(preapprovalId);
        subscription.setStatus(SubscriptionStatus.INCOMPLETE); // Será atualizado pelo status (authorized/approved = ACTIVE)

        log.info("✓ Nova assinatura criada: UserId={} (external_reference), PlanId={}, PreapprovalId={}",
                userId, plan.getId(), preapprovalId);
        return subscription;
    }

    /**
     * Atualiza status da assinatura baseado em preapproval e action.
     * Regra: cancelled/canceled → bloquear usuário (CANCELED); authorized/approved → liberar (ACTIVE).
     */
    private void updateSubscriptionStatusFromPreapproval(Subscription subscription,
            Map<String, Object> preapprovalData,
            String action) {
        String status = (String) preapprovalData.get("status");

        if ("cancelled".equalsIgnoreCase(action) || "canceled".equalsIgnoreCase(action)) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(LocalDateTime.now());
            log.info("Assinatura cancelada via webhook (action={}) → usuário bloqueado: userId={}", action, subscription.getUser() != null ? subscription.getUser().getId() : null);
        } else if ("paused".equalsIgnoreCase(action)) {
            subscription.setStatus(SubscriptionStatus.PAUSED);
            log.info("Assinatura pausada via action: {}", action);
        } else if ("payment_failed".equalsIgnoreCase(action) || "payment_failed".equalsIgnoreCase(status)) {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            log.warn("Falha de pagamento na assinatura (action={}, status={}) → PAST_DUE: subscriptionId={}", action, status, subscription.getId());
        } else {
            // Mapear status do preapproval
            SubscriptionStatus localStatus = mapMercadoPagoStatusToLocal(status);
            subscription.setStatus(localStatus);

            if (localStatus == SubscriptionStatus.ACTIVE) {
                subscription.setCurrentPeriodStart(LocalDateTime.now());
                subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
            }

            log.info("Status da assinatura atualizado: {} -> {}", status, localStatus);
        }
    }

    /**
     * Atualiza período de renovação mensal da assinatura
     */
    private void updateSubscriptionRenewalPeriod(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            LocalDateTime now = LocalDateTime.now();

            // Se current_period_end está no passado ou não definido, atualizar
            if (subscription.getCurrentPeriodEnd() == null ||
                    subscription.getCurrentPeriodEnd().isBefore(now)) {
                subscription.setCurrentPeriodStart(now);
                subscription.setCurrentPeriodEnd(now.plusMonths(1)); // Renovação mensal
                log.info("Período de renovação atualizado: {} até {}",
                        subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd());
            }
        }
    }

    /**
     * Mapeia status do Mercado Pago para PaymentStatus
     * Conforme documentação oficial do Mercado Pago Payments API:
     * - approved: Pagamento aprovado
     * - rejected: Pagamento rejeitado
     * - pending/in_process: Pagamento pendente
     * - cancelled/canceled: Pagamento cancelado
     * - refunded: Pagamento reembolsado
     * - charged_back: Estorno (chargeback)
     */
    private Payment.PaymentStatus mapMercadoPagoStatusToPaymentStatus(String mercadoPagoStatus) {
        if (mercadoPagoStatus == null) {
            return Payment.PaymentStatus.PENDING;
        }

        return switch (mercadoPagoStatus.toLowerCase()) {
            case "approved" -> Payment.PaymentStatus.APPROVED;
            case "pending", "in_process" -> Payment.PaymentStatus.PENDING;
            case "rejected" -> Payment.PaymentStatus.REJECTED;
            case "cancelled", "canceled" -> Payment.PaymentStatus.CANCELLED;
            case "refunded" -> Payment.PaymentStatus.REFUNDED;
            case "charged_back" -> Payment.PaymentStatus.CHARGED_BACK;
            default -> {
                log.warn("Status desconhecido do Mercado Pago: {}, usando PENDING", mercadoPagoStatus);
                yield Payment.PaymentStatus.PENDING;
            }
        };
    }

    /**
     * Mapeia status do Mercado Pago para status local (SubscriptionStatus)
     * Conforme documentação oficial (2025):
     * - approved: Pagamento aprovado
     * - rejected: Pagamento rejeitado
     * - pending/in_process: Pagamento pendente
     * - cancelled: Pagamento cancelado
     * - refunded: Pagamento reembolsado
     * - charged_back: Estorno (chargeback)
     */
    private SubscriptionStatus mapMercadoPagoStatusToLocal(String mercadoPagoStatus) {
        return switch (mercadoPagoStatus != null ? mercadoPagoStatus.toLowerCase() : "") {
            case "approved", "authorized", "active" -> SubscriptionStatus.ACTIVE;
            case "pending", "in_process" -> SubscriptionStatus.INCOMPLETE;
            case "cancelled", "canceled" -> SubscriptionStatus.CANCELED;
            case "paused", "rejected", "payment_failed" -> SubscriptionStatus.PAST_DUE;
            case "refunded", "charged_back" -> SubscriptionStatus.CANCELED;
            default -> {
                log.warn("Status desconhecido do Mercado Pago: {}", mercadoPagoStatus);
                yield SubscriptionStatus.EXPIRED;
            }
        };
    }

    /**
     * Converte Date do Mercado Pago para LocalDateTime
     */
    private LocalDateTime convertDateToLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Converte objeto de data (Date, OffsetDateTime, etc) para LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        if (dateObj instanceof Date) {
            return convertDateToLocalDateTime((Date) dateObj);
        } else if (dateObj instanceof OffsetDateTime) {
            return ((OffsetDateTime) dateObj).toLocalDateTime();
        } else if (dateObj instanceof LocalDateTime) {
            return (LocalDateTime) dateObj;
        } else {
            log.warn("Tipo de data não suportado: {}", dateObj.getClass().getName());
            return LocalDateTime.now();
        }
    }

    /**
     * Converte payload Map para JSON string para validação de assinatura
     */
    private String convertPayloadToString(Map<String, Object> payload) {
        try {
            // Usar Jackson ObjectMapper se disponível, senão usar toString simples
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Erro ao converter payload para JSON: {}", e.getMessage());
            return payload.toString();
        }
    }
}
