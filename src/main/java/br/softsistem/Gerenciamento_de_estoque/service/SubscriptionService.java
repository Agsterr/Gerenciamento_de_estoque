package br.softsistem.Gerenciamento_de_estoque.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.softsistem.Gerenciamento_de_estoque.config.AsaasConfig;
import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.config.PaymentProviderConfig;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AsaasPaymentMode;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.exception.SubscriptionPersistenceException;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;

/**
 * Service para gerenciamento de assinaturas (Asaas como provedor padrão).
 */
@Service
@Transactional
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final int PAID_PERIOD_DAYS = 30;

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentProviderConfig paymentProviderConfig;
    private final AsaasService asaasService;
    private final AsaasConfig asaasConfig;
    private final MercadoPagoConfig mercadoPagoConfig;
    private final MercadoPagoService mercadoPagoService;
    private final MercadoPagoPlanService mercadoPagoPlanService;
    private final TrialSubscriptionService trialSubscriptionService;

    @Autowired
    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            UsuarioRepository usuarioRepository,
            PaymentProviderConfig paymentProviderConfig,
            AsaasService asaasService,
            AsaasConfig asaasConfig,
            MercadoPagoConfig mercadoPagoConfig,
            TrialSubscriptionService trialSubscriptionService,
            @Autowired(required = false) MercadoPagoService mercadoPagoService,
            @Autowired(required = false) MercadoPagoPlanService mercadoPagoPlanService) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.usuarioRepository = usuarioRepository;
        this.paymentProviderConfig = paymentProviderConfig;
        this.asaasService = asaasService;
        this.asaasConfig = asaasConfig;
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.trialSubscriptionService = trialSubscriptionService;
        this.mercadoPagoService = mercadoPagoService;
        this.mercadoPagoPlanService = mercadoPagoPlanService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS PRINCIPAIS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Assinatura visível ao usuário: TRIAL, ACTIVE ou INCOMPLETE com cobrança Asaas pendente.
     */
    public Optional<Subscription> getCurrentSubscription(Long userId) {
        Optional<Subscription> trialOrActive = subscriptionRepository.findByUserIdAndStatusIn(
                userId,
                List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE));
        if (trialOrActive.isPresent()) {
            return trialOrActive;
        }
        return subscriptionRepository.findByUserId(userId)
                .filter(sub -> sub.getStatus() == SubscriptionStatus.INCOMPLETE
                        && sub.getAsaasPaymentId() != null
                        && !sub.getAsaasPaymentId().isBlank());
    }

    /**
     * ⭐ MÉTODO PRINCIPAL - Cria assinatura via Mercado Pago ⭐
     *
     * FLUXO CORRETO:
     * 1. Valida usuário e plano
     * 2. Cria subscription local (status INCOMPLETE)
     * 3. Chama Mercado Pago para gerar checkout (COM free trial)
     * 4. Retorna URL do checkout
     *
     * Quando cardTokenId não é enviado: usa link direto do plano (usuário preenche cartão na página do MP).
     * Quando cardTokenId é enviado (Checkout Transparente): chama a API do MP para criar a assinatura com o cartão.
     *
     * @param payerEmail E-mail do pagador (opcional; se null, usa user.getEmail())
     */
    public Map<String, Object> createSubscriptionForUser(String planIdInput, String backUrl, String cardTokenId, String payerEmail) {
        return createSubscriptionForUser(planIdInput, backUrl, cardTokenId, payerEmail, null);
    }

    /**
     * Cria checkout de assinatura. Com Asaas gera link de pagamento (invoiceUrl).
     */
    public Map<String, Object> createSubscriptionForUser(String planIdInput, String backUrl, String cardTokenId, String payerEmail, String cpfCnpj) {
        return createSubscriptionForUser(planIdInput, backUrl, cardTokenId, payerEmail, cpfCnpj, AsaasPaymentMode.RECURRING);
    }

    /**
     * Cria checkout de assinatura. Com Asaas gera assinatura recorrente ou cobrança avulsa (Pix/boleto).
     */
    public Map<String, Object> createSubscriptionForUser(String planIdInput, String backUrl, String cardTokenId,
            String payerEmail, String cpfCnpj, AsaasPaymentMode paymentMode) {
        if (paymentProviderConfig.isAsaas()) {
            return createAsaasCheckout(planIdInput, cpfCnpj, paymentMode != null ? paymentMode : AsaasPaymentMode.RECURRING);
        }
        return createMercadoPagoCheckout(planIdInput, backUrl, cardTokenId, payerEmail);
    }

    private Map<String, Object> createAsaasCheckout(String planIdInput, String cpfCnpj, AsaasPaymentMode paymentMode) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Checkout Asaas: user={} plan={} mode={}", userId, planIdInput, paymentMode);

        Optional<Subscription> active = getCurrentSubscription(userId);
        if (active.isPresent() && active.get().getStatus() == SubscriptionStatus.ACTIVE) {
            LocalDateTime periodEnd = active.get().getCurrentPeriodEnd();
            if (periodEnd != null && LocalDateTime.now().isBefore(periodEnd)) {
                throw new IllegalStateException("Você já possui uma assinatura ativa");
            }
        }

        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        Plan plan = findPlanByIdOrMpId(planIdInput);
        if (plan == null) {
            throw new IllegalArgumentException("Plano não encontrado ou inativo: " + planIdInput);
        }

        Subscription subscription = subscriptionRepository.findByUserId(userId).orElseGet(Subscription::new);
        if (subscription.getId() == null) {
            subscription.setUser(user);
            subscription.setPlan(plan);
        } else {
            subscription.setPlan(plan);
        }

        boolean keepTrialAccess = subscription.getStatus() == SubscriptionStatus.TRIAL
                && subscription.getTrialEnd() != null
                && LocalDateTime.now().isBefore(subscription.getTrialEnd());
        if (!keepTrialAccess) {
            subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        }
        subscription.setPaymentProvider("ASAAS");
        subscription.setPaymentMode(paymentMode.name());
        Subscription saved = subscriptionRepository.save(subscription);

        asaasService.ensureCustomer(user, cpfCnpj);
        usuarioRepository.save(user);

        Map<String, Object> paymentResponse;
        if (paymentMode == AsaasPaymentMode.RECURRING) {
            paymentResponse = asaasService.createRecurringSubscription(user, plan, saved, cpfCnpj);
        } else {
            paymentResponse = asaasService.createMonthlyCharge(user, plan, saved, cpfCnpj, paymentMode.getBillingType());
            saved.setAsaasSubscriptionId(null);
        }

        String asaasSubscriptionId = paymentMode == AsaasPaymentMode.RECURRING
                ? mapGetString(paymentResponse, "asaasSubscriptionId")
                : null;
        if (asaasSubscriptionId == null && paymentMode == AsaasPaymentMode.RECURRING) {
            asaasSubscriptionId = mapGetString(paymentResponse, "id");
        }
        String paymentId = mapGetString(paymentResponse, "id");
        String invoiceUrl = asaasService.resolveInvoiceUrl(paymentResponse);
        if (invoiceUrl == null || invoiceUrl.isBlank()) {
            invoiceUrl = mapGetString(paymentResponse, "invoiceUrl");
        }
        if (invoiceUrl == null || invoiceUrl.isBlank()) {
            invoiceUrl = mapGetString(paymentResponse, "bankSlipUrl");
        }

        saved.setAsaasSubscriptionId(asaasSubscriptionId);
        saved.setAsaasPaymentId(paymentId);
        saved.setAsaasCustomerId(user.getAsaasCustomerId());
        saved.setCheckoutUrl(invoiceUrl);
        subscriptionRepository.save(saved);

        Map<String, Object> response = new HashMap<>();
        response.put("checkoutUrl", invoiceUrl);
        response.put("paymentUrl", invoiceUrl);
        response.put("sessionId", paymentId);
        response.put("subscriptionId", saved.getId());
        response.put("status", saved.getStatus().name());
        response.put("testMode", asaasConfig.isSandbox());
        response.put("paymentProvider", "ASAAS");
        response.put("paymentMode", paymentMode.name());
        response.put("billingType", paymentMode.getBillingType());
        response.put("asaasPaymentId", paymentId);
        response.put("asaasSubscriptionId", asaasSubscriptionId);
        response.put("transparentCheckout", paymentMode != AsaasPaymentMode.RECURRING);
        putIfPresent(response, "pixQrCodeImage", paymentResponse.get("pixQrCodeImage"));
        putIfPresent(response, "pixCopyPaste", paymentResponse.get("pixCopyPaste"));
        putIfPresent(response, "pixExpirationDate", paymentResponse.get("pixExpirationDate"));
        putIfPresent(response, "bankSlipUrl", paymentResponse.get("bankSlipUrl"));
        putIfPresent(response, "identificationField", paymentResponse.get("identificationField"));
        putIfPresent(response, "dueDate", paymentResponse.get("dueDate"));
        return response;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            target.put(key, value);
        }
    }

    private Map<String, Object> createMercadoPagoCheckout(String planIdInput, String backUrl, String cardTokenId, String payerEmail) {
        if (mercadoPagoService == null) {
            throw new IllegalStateException("Mercado Pago não está habilitado. Configure app.payment.provider=mercadopago");
        }
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 Criando assinatura");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 User ID: {}", userId);
        log.info("📋 Plan Input: {}", planIdInput);
        log.info("💳 Card token presente: {}", (cardTokenId != null && !cardTokenId.isBlank()));

        // ═══════════════════════════════════════════════════════════════
        // 1. VALIDAÇÕES
        // ═══════════════════════════════════════════════════════════════

        Optional<Subscription> existingSubscription = getCurrentSubscription(userId);
        if (existingSubscription.isPresent() &&
                existingSubscription.get().getStatus() == SubscriptionStatus.ACTIVE) {
            log.warn("⚠️ Usuário {} já tem assinatura ativa", userId);
            throw new IllegalStateException("Você já possui uma assinatura ativa");
        }

        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        // ═══════════════════════════════════════════════════════════════
        // 2. BUSCA PLANO (por ID do MP ou ID local)
        // ═══════════════════════════════════════════════════════════════

        Plan plan = findPlanByIdOrMpId(planIdInput);

        if (plan == null) {
            log.error("❌ Plano não encontrado: {}", planIdInput);
            throw new IllegalArgumentException("Plano não encontrado ou inativo: " + planIdInput);
        }

        log.info("✅ Plano encontrado:");
        log.info("  - ID Local: {}", plan.getId());
        log.info("  - Nome: {}", plan.getName());
        log.info("  - Preço: R$ {}", plan.getPrice());
        log.info("  - MP Plan ID: {}", plan.getMercadoPagoPreapprovalPlanId());

        // ═══════════════════════════════════════════════════════════════
        // 3. CRIA SUBSCRIPTION LOCAL (status: INCOMPLETE)
        // ═══════════════════════════════════════════════════════════════

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        Subscription saved = subscriptionRepository.save(subscription);

        log.info("✅ Subscription local criada: ID={}", saved.getId());

        // ═══════════════════════════════════════════════════════════════
        // 4. CHECKOUT: API (com card_token_id) OU link direto do plano
        // ═══════════════════════════════════════════════════════════════

        String checkoutUrl;
        boolean hasCardToken = cardTokenId != null && !cardTokenId.isBlank();
        String mpPlanId = plan.getMercadoPagoPreapprovalPlanId();

        if (hasCardToken) {
            // Checkout Transparente: com token de cartão, chama a API do Mercado Pago (não redireciona)
            try {
                String emailForPayer = (payerEmail != null && !payerEmail.isBlank()) ? payerEmail : user.getEmail();
                Map<String, Object> preapprovalResponse = mercadoPagoService.createPreapproval(
                        user, plan, saved.getId(), cardTokenId, emailForPayer);

                String preapprovalId = mapGetString(preapprovalResponse, "id");
                if (preapprovalId == null || preapprovalId.isBlank()) {
                    throw new RuntimeException("Mercado Pago não retornou ID da assinatura");
                }

                checkoutUrl = null;
                saved.setMercadoPagoSubscriptionId(preapprovalId);
                String initPoint = mapGetString(preapprovalResponse, "init_point");
                saved.setCheckoutUrl(initPoint != null ? initPoint : "");

                try {
                    subscriptionRepository.save(saved);
                } catch (Exception persistEx) {
                    log.error("❌ Assinatura criada no MP (id={}) mas falha ao salvar no banco: {}", preapprovalId, persistEx.getMessage(), persistEx);
                    throw new SubscriptionPersistenceException(
                            "Assinatura criada no Mercado Pago, mas houve falha ao registrar localmente. Não reenvie o pagamento; entre em contato com o suporte e informe o ID: " + preapprovalId,
                            preapprovalId, persistEx);
                }

                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("✅ SUCESSO! Checkout Transparente - assinatura criada via API");
                log.info("🆔 Preapproval ID: {}", preapprovalId);
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } catch (SubscriptionPersistenceException e) {
                throw e;
            } catch (Exception e) {
                log.error("❌ Erro ao criar preapproval no MP: {}", e.getMessage(), e);
                throw new RuntimeException("Erro ao criar assinatura: " + e.getMessage(), e);
            }
        } else if (mpPlanId != null && !mpPlanId.isBlank()) {
            // Sem token: cria assinatura via POST /preapproval (status=pending) e redireciona para init_point
            try {
                Map<String, Object> preapprovalResponse = mercadoPagoService.createPreapproval(
                        user, plan, saved.getId(), null, null);

                String preapprovalId = mapGetString(preapprovalResponse, "id");
                if (preapprovalId == null || preapprovalId.isBlank()) {
                    throw new RuntimeException("Mercado Pago não retornou ID da assinatura");
                }

                String initPoint = mapGetString(preapprovalResponse, "init_point");
                String sandboxInit = mapGetString(preapprovalResponse, "sandbox_init_point");
                checkoutUrl = (mercadoPagoConfig.isProduction() || sandboxInit == null || sandboxInit.isBlank())
                        ? initPoint
                        : sandboxInit;
                if (checkoutUrl == null || checkoutUrl.isBlank()) {
                    checkoutUrl = initPoint;
                }

                saved.setMercadoPagoSubscriptionId(preapprovalId);
                saved.setCheckoutUrl(checkoutUrl != null ? checkoutUrl : "");
                try {
                    subscriptionRepository.save(saved);
                } catch (Exception persistEx) {
                    log.error("❌ Assinatura criada no MP (id={}) mas falha ao salvar no banco: {}", preapprovalId, persistEx.getMessage(), persistEx);
                    throw new SubscriptionPersistenceException(
                            "Assinatura criada no Mercado Pago, mas houve falha ao registrar localmente. Não repita o checkout; entre em contato com o suporte e informe o ID: " + preapprovalId,
                            preapprovalId, persistEx);
                }

                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("✅ Assinatura criada no MP (POST /preapproval); redirecionar para init_point");
                log.info("🆔 Preapproval ID: {}", preapprovalId);
                log.info("🔗 Checkout URL (init_point): {}", checkoutUrl);
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } catch (SubscriptionPersistenceException e) {
                throw e;
            } catch (Exception e) {
                log.error("❌ Erro ao criar preapproval no MP: {}", e.getMessage(), e);
                throw new RuntimeException("Erro ao criar assinatura: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Configure o ID do plano no painel do Mercado Pago (mercadoPagoPreapprovalPlanId) para este plano.");
        }

        // ═══════════════════════════════════════════════════════════════
        // 5. RETORNA RESPOSTA
        // ═══════════════════════════════════════════════════════════════

        Map<String, Object> response = new HashMap<>();
        response.put("checkoutUrl", checkoutUrl);
        response.put("sessionId", checkoutUrl);
        response.put("subscriptionId", saved.getId());
        response.put("status", saved.getStatus().name());
        response.put("testMode", !mercadoPagoConfig.isProduction());
        if (saved.getMercadoPagoSubscriptionId() != null) {
            response.put("preapprovalId", saved.getMercadoPagoSubscriptionId());
        }
        if (hasCardToken) {
            response.put("transparentCheckout", true);
        }

        return response;
    }

    /**
     * Ativa assinatura após confirmação de pagamento via webhook Asaas.
     */
    public void activateFromAsaasPayment(Subscription subscription, Map<String, Object> payment) {
        LocalDateTime now = LocalDateTime.now();
        String paymentId = mapGetString(payment, "id");
        if (paymentId != null && !paymentId.isBlank()) {
            subscription.setAsaasPaymentId(paymentId);
        }

        if (subscription.getStatus() == SubscriptionStatus.TRIAL
                || subscription.getStatus() == SubscriptionStatus.INCOMPLETE) {
            subscription.setTrialEnd(now);
        }

        LocalDateTime periodEndBase;
        LocalDateTime periodStart;
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(now)) {
            periodStart = subscription.getCurrentPeriodStart() != null ? subscription.getCurrentPeriodStart() : now;
            periodEndBase = subscription.getCurrentPeriodEnd();
            log.info("Renovando assinatura {} — estendendo a partir de {}", subscription.getId(), periodEndBase);
        } else {
            periodStart = now;
            periodEndBase = now;
            log.info("Ativando assinatura {} via Asaas", subscription.getId());
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEndBase.plusDays(PAID_PERIOD_DAYS));
        subscription.setAccessBlocked(false);
        subscriptionRepository.save(subscription);
        log.info("Assinatura {} vigente até {}", subscription.getId(), subscription.getCurrentPeriodEnd());
    }

    /** Extrai String do Map (resposta do MP pode ter id como número no JSON). */
    private static String mapGetString(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null) return null;
        if (v instanceof String) return (String) v;
        return String.valueOf(v);
    }

    /**
     * Busca plano por ID do Mercado Pago OU ID local
     */
    private Plan findPlanByIdOrMpId(String planIdInput) {
        // Tenta por ID do Mercado Pago primeiro
        Optional<Plan> planOpt = planRepository.findByMercadoPagoPreapprovalPlanIdAndIsActiveTrue(planIdInput);

        if (planOpt.isPresent()) {
            log.info("📦 Plano encontrado por MP ID");
            return planOpt.get();
        }

        // Fallback: Tenta por ID local
        try {
            Long planId = Long.parseLong(planIdInput);
            Plan plan = planRepository.findById(planId)
                    .filter(Plan::getIsActive)
                    .orElse(null);

            if (plan != null) {
                log.info("📦 Plano encontrado por ID local");
            }

            return plan;
        } catch (NumberFormatException e) {
            log.warn("⚠️ Plan ID não é numérico: {}", planIdInput);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCELAMENTO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cancela assinatura local E no Mercado Pago
     */
    public void cancelSubscription(Long userId) {
        log.info("🚫 Cancelando assinatura do usuário {}", userId);

        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);
        if (subscriptionOpt.isEmpty()) {
            throw new IllegalStateException("Usuário não possui assinatura ativa");
        }

        Subscription subscription = subscriptionOpt.get();

        // Cancela no Asaas (assinatura recorrente)
        if (subscription.getAsaasSubscriptionId() != null && !subscription.getAsaasSubscriptionId().isBlank()) {
            try {
                asaasService.cancelRecurringSubscription(subscription.getAsaasSubscriptionId());
                log.info("✅ Assinatura recorrente cancelada no Asaas");
            } catch (Exception e) {
                log.warn("⚠️ Erro ao cancelar assinatura no Asaas: {}", e.getMessage());
            }
        }

        // Cancela no Mercado Pago (se tiver ID)
        if (subscription.getMercadoPagoSubscriptionId() != null && mercadoPagoService != null) {
            try {
                mercadoPagoService.cancelPreapproval(subscription.getMercadoPagoSubscriptionId());
                log.info("✅ Cancelado no Mercado Pago");
            } catch (Exception e) {
                log.warn("⚠️ Erro ao cancelar no MP: {}", e.getMessage());
            }
        }

        // Cancela localmente
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("✅ Assinatura cancelada com sucesso");
    }

    /**
     * Cancela pelo ID do Mercado Pago (preapproval_id)
     */
    public void cancelSubscriptionByPreapprovalId(String preapprovalId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("🚫 Cancelamento por preapproval_id: {} (user: {})", preapprovalId, userId);

        Subscription subscription = subscriptionRepository.findByMercadoPagoSubscriptionId(preapprovalId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada"));

        // Validação de segurança
        if (!subscription.getUser().getId().equals(userId)) {
            log.warn("⚠️ Tentativa de cancelar assinatura de outro usuário");
            throw new SecurityException("Sem permissão para cancelar esta assinatura");
        }

        // Idempotência
        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            log.info("ℹ️ Assinatura já cancelada (idempotente)");
            return;
        }

        // Cancela no MP
        if (mercadoPagoService != null) {
            mercadoPagoService.cancelPreapproval(preapprovalId);
        }

        // Atualiza localmente
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("✅ Assinatura {} cancelada", preapprovalId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ATIVAÇÃO E RENOVAÇÃO (Webhooks)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Aplica pagamento aprovado (chamado por webhook)
     */
    public void applyApprovedPayment(String externalReference, Long mercadoPagoPaymentId) {
        log.info("💰 Aplicando pagamento aprovado");
        log.info("  - external_reference: {}", externalReference);
        log.info("  - payment_id: {}", mercadoPagoPaymentId);

        if (externalReference == null || externalReference.isBlank()) {
            log.warn("⚠️ external_reference vazio, ignorando");
            return;
        }

        // external_reference = ID do usuário logado (formato: "123" ou legado "userId:planId")
        String[] parts = externalReference.split(":");
        Long userId;
        Long planId;

        try {
            userId = Long.parseLong(parts[0]);
            if (parts.length >= 2) {
                planId = Long.parseLong(parts[1]);
            } else {
                // Só userId: busca plano da assinatura existente do usuário
                planId = subscriptionRepository.findByUserId(userId)
                        .map(s -> s.getPlan() != null ? s.getPlan().getId() : null)
                        .orElse(null);
                if (planId == null) {
                    log.warn("⚠️ external_reference só tem userId e usuário não tem assinatura com plano: {}", externalReference);
                    return;
                }
            }
            activateOrRenewSubscription(userId, planId, mercadoPagoPaymentId);
        } catch (NumberFormatException e) {
            log.error("❌ IDs não numéricos em external_reference: {}", externalReference);
        }
    }

    /**
     * Ativa ou renova assinatura após pagamento
     */
    public void activateOrRenewSubscription(Long userId, Long planId, Long mercadoPagoPaymentId) {
        log.info("✅ Ativando/renovando assinatura");
        log.info("  - userId: {}", userId);
        log.info("  - planId: {}", planId);
        log.info("  - paymentId: {}", mercadoPagoPaymentId);

        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        Optional<Subscription> existingOpt = subscriptionRepository.findByUserId(userId);
        Subscription subscription = existingOpt.orElseGet(Subscription::new);

        if (existingOpt.isEmpty()) {
            subscription.setUser(user);
        }

        subscription.setPlan(plan);

        // Encerra trial se existir
        if (subscription.getStatus() == SubscriptionStatus.TRIAL) {
            subscription.setTrialEnd(LocalDateTime.now());
            log.info("  - Trial encerrado");
        }

        // Ativa assinatura
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusDays(30));

        subscriptionRepository.save(subscription);

        log.info("✅ Assinatura ativada até {}", subscription.getCurrentPeriodEnd());
    }

    /**
     * Atualiza status da assinatura
     */
    public void updateSubscriptionStatusByUserId(Long userId, SubscriptionStatus status) {
        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("⚠️ Usuário {} sem assinatura ativa", userId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(status);

        LocalDateTime now = LocalDateTime.now();

        switch (status) {
            case ACTIVE:
                subscription.setCurrentPeriodStart(now);
                subscription.setCurrentPeriodEnd(now.plusDays(30));
                break;
            case CANCELED:
                subscription.setCanceledAt(now);
                break;
            case EXPIRED:
                subscription.setEndedAt(now);
                break;
            default:
                break;
        }

        subscriptionRepository.save(subscription);
        log.info("✅ Status atualizado para {}", status);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONSULTAS E VALIDAÇÕES
    // ═══════════════════════════════════════════════════════════════════════

    public Subscription getCurrentSubscriptionForUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }

        Optional<Subscription> existing = getCurrentSubscription(userId);
        if (existing.isPresent()) {
            return normalizeTrialSubscription(existing.get());
        }

        if (hasSubscriptionBypass(userId)) {
            return null;
        }

        return usuarioRepository.findById(userId)
                .map(trialSubscriptionService::startTrialForUser)
                .orElse(null);
    }

    private Subscription normalizeTrialSubscription(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
            return subscription;
        }

        boolean updated = false;
        if (subscription.getTrialStart() == null) {
            LocalDateTime base = subscription.getCreatedAt() != null
                    ? subscription.getCreatedAt()
                    : LocalDateTime.now();
            subscription.setTrialStart(base);
            updated = true;
        }
        if (subscription.getTrialEnd() == null) {
            subscription.setTrialEnd(subscription.getTrialStart().plusDays(trialSubscriptionService.getTrialDays()));
            updated = true;
        }
        if (subscription.getCurrentPeriodStart() == null) {
            subscription.setCurrentPeriodStart(subscription.getTrialStart());
            updated = true;
        }
        if (subscription.getCurrentPeriodEnd() == null) {
            subscription.setCurrentPeriodEnd(subscription.getTrialEnd());
            updated = true;
        }

        return updated ? subscriptionRepository.save(subscription) : subscription;
    }

    /**
     * Consulta status da cobrança no Asaas e ativa a assinatura se o pagamento foi confirmado
     * (fallback quando o webhook ainda não chegou — comum em dev/sandbox).
     */
    public Subscription syncAsaasPaymentForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        Optional<Subscription> subOpt = subscriptionRepository.findByUserId(userId);
        if (subOpt.isEmpty()) {
            return getCurrentSubscriptionForUser();
        }
        Subscription sub = subOpt.get();
        if (sub.getAsaasPaymentId() == null || sub.getAsaasPaymentId().isBlank()) {
            return getCurrentSubscriptionForUser();
        }
        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return sub;
        }
        try {
            Map<String, Object> payment = asaasService.getPayment(sub.getAsaasPaymentId());
            if (!asaasService.isPaymentConfirmed(payment) && asaasConfig.isSandbox()) {
                log.info("Tentando confirmar pagamento sandbox via API: payment={}", sub.getAsaasPaymentId());
                payment = asaasService.confirmSandboxPayment(sub.getAsaasPaymentId());
            }
            if (asaasService.isPaymentConfirmed(payment)) {
                activateFromAsaasPayment(sub, payment);
                log.info("Assinatura {} ativada via sync Asaas payment={}", sub.getId(), sub.getAsaasPaymentId());
            }
        } catch (Exception e) {
            log.warn("Falha ao sincronizar pagamento Asaas para user={}: {}", userId, e.getMessage());
        }
        return getCurrentSubscriptionForUser();
    }

    public List<Subscription> getSubscriptionHistoryForUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Map<String, String> cancelSubscriptionForUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        cancelSubscription(userId);
        return Map.of("message", "Assinatura cancelada com sucesso");
    }

    public boolean canUserAccess(Long userId, String feature) {
        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);

        if (subscriptionOpt.isEmpty()) {
            return false;
        }

        Plan plan = subscriptionOpt.get().getPlan();

        return switch (feature.toLowerCase()) {
            case "reports" -> Boolean.TRUE.equals(plan.getHasReports());
            case "analytics" -> Boolean.TRUE.equals(plan.getHasAdvancedAnalytics());
            case "api_access" -> Boolean.TRUE.equals(plan.getHasApiAccess());
            default -> true;
        };
    }

    public Map<String, Boolean> checkFeatureAccessForUser(String feature) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean hasAccess = canUserAccess(userId, feature);
        return Map.of("hasAccess", hasAccess);
    }

    public boolean isWithinLimits(Long userId, String limitType, int currentCount) {
        if (hasSubscriptionBypass(userId)) {
            return true;
        }

        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);

        if (subscriptionOpt.isEmpty()) {
            return false;
        }

        Plan plan = subscriptionOpt.get().getPlan();

        return switch (limitType.toLowerCase()) {
            case "users" -> plan.getMaxUsers() == null || currentCount < plan.getMaxUsers();
            case "products" -> true;
            default -> true;
        };
    }

    public boolean hasSubscriptionBypass(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario principal) {
            if (userId == null || userId.equals(principal.getId())) {
                return principal.hasSubscriptionBypass();
            }
        }

        if (userId == null) {
            return false;
        }

        return usuarioRepository.findById(userId)
                .map(Usuario::hasSubscriptionBypass)
                .orElse(false);
    }

    public Map<String, Object> checkUsageLimitsForUser(String limitType, int currentCount) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean withinLimits = isWithinLimits(userId, limitType, currentCount);

        Map<String, Object> response = new HashMap<>();
        response.put("withinLimits", withinLimits);
        response.put("currentCount", currentCount);
        return response;
    }

    public List<Subscription> getAllSubscriptionsForAdmin() {
        return subscriptionRepository.findAll();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAREFAS AGENDADAS (Cron Jobs)
    // ═══════════════════════════════════════════════════════════════════════

    public void sendTrialEndingAlerts() {
        log.info("📧 Enviando alertas de fim de trial...");

        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        List<Subscription> trialsEndingSoon = subscriptionRepository.findTrialsEndingSoon(threeDaysFromNow);

        for (Subscription subscription : trialsEndingSoon) {
            try {
                log.info("  ✉️ Alerta para: {}", subscription.getUser().getEmail());
                // TODO: emailService.sendTrialEndingAlert(subscription);

                subscription.setTrialWarningSent(true);
                subscriptionRepository.save(subscription);

            } catch (Exception e) {
                log.error("❌ Erro ao enviar alerta: {}", e.getMessage());
            }
        }

        log.info("✅ {} alertas enviados", trialsEndingSoon.size());
    }

    public void processExpiredTrials() {
        log.info("🔄 Processando trials expirados...");

        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredTrials = subscriptionRepository.findExpiredTrials(now);

        for (Subscription subscription : expiredTrials) {
            try {
                subscription.setStatus(SubscriptionStatus.CANCELED);
                subscription.setEndedAt(now);
                subscriptionRepository.save(subscription);

                log.info("  ✅ Trial expirado: user={}", subscription.getUser().getId());

            } catch (Exception e) {
                log.error("❌ Erro ao processar: {}", e.getMessage());
            }
        }

        log.info("✅ {} trials processados", expiredTrials.size());
    }

    public int cleanupOldCancelledSubscriptions(LocalDateTime cutoffDate) {
        log.info("🧹 Limpando assinaturas canceladas antes de {}", cutoffDate);

        List<Subscription> old = subscriptionRepository
                .findByStatusAndEndedAtBefore(SubscriptionStatus.CANCELED, cutoffDate);

        int count = old.size();

        if (count > 0) {
            subscriptionRepository.deleteAll(old);
            log.info("✅ {} assinaturas removidas", count);
        }

        return count;
    }

    public Map<String, Long> generateDailyReport() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Map<String, Long> report = new HashMap<>();

        report.put("active", (long) subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).size());
        report.put("trials", (long) subscriptionRepository.findByStatus(SubscriptionStatus.TRIAL).size());
        report.put("cancelled_today", (long) subscriptionRepository
                .findByStatusAndEndedAtBetween(SubscriptionStatus.CANCELED, startOfDay, endOfDay).size());
        report.put("new_today", (long) subscriptionRepository
                .findByCreatedAtBetween(startOfDay, endOfDay).size());

        return report;
    }
}