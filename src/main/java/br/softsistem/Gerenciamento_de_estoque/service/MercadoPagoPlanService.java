package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service para buscar e gerenciar planos do Mercado Pago
 * REFATORADO: Logs detalhados e tratamento de erros robusto
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
public class MercadoPagoPlanService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPlanService.class);

    private final MercadoPagoConfig mercadoPagoConfig;
    private final RestTemplate restTemplate;

    public MercadoPagoPlanService(MercadoPagoConfig mercadoPagoConfig) {
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Busca todos os planos ativos no Mercado Pago
     *
     * @return Lista de planos com id, name, price, frequency, status, free_trial
     */
    public List<Map<String, Object>> getPlans() {
        return getPlans(null);
    }

    /**
     * Busca planos no Mercado Pago com filtro opcional de status
     *
     * @param statusFilter Status para filtrar (active, paused, cancelled) ou null para todos
     * @return Lista de planos
     */
    public List<Map<String, Object>> getPlans(String statusFilter) {
        log.info("🔍 Buscando planos no Mercado Pago... (filter: {})",
                statusFilter != null ? statusFilter : "todos");

        String url = "https://api.mercadopago.com/preapproval_plan/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mercadoPagoConfig.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response;

        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("✅ Resposta da API recebida: status={}", response.getStatusCode());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ Erro HTTP ao buscar planos: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 401) {
                log.error("🔑 Token de acesso inválido ou expirado!");
            } else if (e.getStatusCode().value() == 404) {
                log.warn("⚠️ Endpoint não encontrado - verifique a URL da API");
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erro inesperado ao buscar planos: {}", e.getMessage(), e);
            return Collections.emptyList();
        }

        Map<String, Object> body = response.getBody();

        if (body == null) {
            log.warn("⚠️ Resposta vazia da API");
            return Collections.emptyList();
        }

        // Processa resultados
        List<Map<String, Object>> plans = new ArrayList<>();

        if (body.get("results") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");

            log.info("📦 {} plano(s) encontrado(s)", results.size());

            for (Map<String, Object> planData : results) {
                try {
                    Map<String, Object> plan = parsePlan(planData);

                    // Filtra por status se especificado
                    if (statusFilter == null || statusFilter.equals(plan.get("status"))) {
                        plans.add(plan);

                        log.debug("  ✓ Plano: id={} name='{}' price={} status={}",
                                plan.get("id"),
                                plan.get("name"),
                                plan.get("price"),
                                plan.get("status"));
                    }

                } catch (Exception e) {
                    log.warn("⚠️ Erro ao processar plano: {}", e.getMessage());
                    // Continua processando outros planos
                }
            }

            log.info("✅ {} plano(s) processado(s) com sucesso", plans.size());

        } else {
            log.warn("⚠️ Campo 'results' não encontrado ou inválido na resposta: {}", body);
        }

        return plans;
    }

    /**
     * Busca um plano específico pelo ID
     *
     * @param planId ID do plano no Mercado Pago
     * @return Dados do plano ou null se não encontrado
     */
    public Map<String, Object> getPlanById(String planId) {
        log.info("🔍 Buscando plano específico: id={}", planId);

        String url = "https://api.mercadopago.com/preapproval_plan/" + planId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mercadoPagoConfig.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> planData = response.getBody();

            if (planData != null) {
                Map<String, Object> plan = parsePlan(planData);
                log.info("✅ Plano encontrado: {}", plan.get("name"));
                return plan;
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("⚠️ Plano {} não encontrado", planId);
            } else {
                log.error("❌ Erro ao buscar plano {}: {}", planId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao buscar plano {}: {}", planId, e.getMessage());
        }

        return null;
    }

    /**
     * Busca apenas planos ativos (status = active)
     *
     * @return Lista de planos ativos
     */
    public List<Map<String, Object>> getActivePlans() {
        log.info("🔍 Buscando apenas planos ATIVOS...");
        return getPlans("active");
    }

    /**
     * Verifica se um plano existe e está ativo
     *
     * @param planId ID do plano
     * @return true se existe e está ativo, false caso contrário
     */
    public boolean isPlanActive(String planId) {
        Map<String, Object> plan = getPlanById(planId);

        if (plan == null) {
            return false;
        }

        String status = (String) plan.get("status");
        boolean isActive = "active".equalsIgnoreCase(status);

        log.info("📋 Plano {} está {}", planId, isActive ? "ATIVO" : "INATIVO (" + status + ")");

        return isActive;
    }

    /**
     * Parseia os dados brutos do plano do Mercado Pago
     *
     * @param planData Dados brutos da API
     * @return Map com dados estruturados do plano
     */
    private Map<String, Object> parsePlan(Map<String, Object> planData) {
        Map<String, Object> plan = new HashMap<>();

        // Dados básicos
        plan.put("id", planData.get("id"));
        plan.put("name", planData.get("reason")); // reason = nome do plano
        plan.put("status", planData.get("status"));
        plan.put("date_created", planData.get("date_created"));
        plan.put("last_modified", planData.get("last_modified"));

        // Auto recurring (configuração de recorrência)
        @SuppressWarnings("unchecked")
        Map<String, Object> autoRecurring = (Map<String, Object>) planData.get("auto_recurring");

        if (autoRecurring != null) {
            plan.put("price", autoRecurring.get("transaction_amount"));
            plan.put("currency_id", autoRecurring.get("currency_id"));
            plan.put("frequency", autoRecurring.get("frequency"));
            plan.put("frequency_type", autoRecurring.get("frequency_type"));

            // ⭐ Free trial (se configurado no plano)
            @SuppressWarnings("unchecked")
            Map<String, Object> freeTrial = (Map<String, Object>) autoRecurring.get("free_trial");

            if (freeTrial != null) {
                plan.put("free_trial_frequency", freeTrial.get("frequency"));
                plan.put("free_trial_frequency_type", freeTrial.get("frequency_type"));
                plan.put("has_free_trial", true);

                log.debug("  🎁 Plano com free trial: {} {}",
                        freeTrial.get("frequency"),
                        freeTrial.get("frequency_type"));
            } else {
                plan.put("has_free_trial", false);
            }

            // Billing day
            plan.put("billing_day", autoRecurring.get("billing_day"));
            plan.put("billing_day_proportional", autoRecurring.get("billing_day_proportional"));
        } else {
            log.warn("⚠️ Plano {} sem configuração auto_recurring", planData.get("id"));
        }

        // Back URL
        plan.put("back_url", planData.get("back_url"));

        return plan;
    }

    /**
     * Valida se o plano está configurado corretamente para assinaturas com trial
     *
     * @param planId ID do plano
     * @return true se válido, false caso contrário
     */
    public boolean validatePlanConfiguration(String planId) {
        log.info("🔍 Validando configuração do plano: {}", planId);

        Map<String, Object> plan = getPlanById(planId);

        if (plan == null) {
            log.error("❌ Plano {} não encontrado", planId);
            return false;
        }

        List<String> errors = new ArrayList<>();

        // Verifica status
        if (!"active".equalsIgnoreCase((String) plan.get("status"))) {
            errors.add("Status não é 'active': " + plan.get("status"));
        }

        // Verifica preço
        Object price = plan.get("price");
        if (price == null || (price instanceof Number && ((Number) price).doubleValue() <= 0)) {
            errors.add("Preço inválido ou não configurado");
        }

        // Verifica frequência
        if (plan.get("frequency") == null || plan.get("frequency_type") == null) {
            errors.add("Frequência de cobrança não configurada");
        }

        // Verifica billing_day (obrigatório para mensais)
        if ("months".equals(plan.get("frequency_type")) && plan.get("billing_day") == null) {
            errors.add("Billing day não configurado (obrigatório para planos mensais)");
        }

        if (!errors.isEmpty()) {
            log.error("❌ Plano {} tem {} erro(s):", planId, errors.size());
            errors.forEach(err -> log.error("  - {}", err));
            return false;
        }

        log.info("✅ Plano {} está configurado corretamente", planId);

        // Log detalhado da configuração
        log.info("  📋 Detalhes:");
        log.info("    - Nome: {}", plan.get("name"));
        log.info("    - Preço: {} {}", plan.get("price"), plan.get("currency_id"));
        log.info("    - Frequência: {} {}", plan.get("frequency"), plan.get("frequency_type"));
        log.info("    - Free trial: {}", plan.get("has_free_trial"));

        if (Boolean.TRUE.equals(plan.get("has_free_trial"))) {
            log.info("    - Trial: {} {}",
                    plan.get("free_trial_frequency"),
                    plan.get("free_trial_frequency_type"));
        }

        log.info("    - Billing day: {}", plan.get("billing_day"));

        return true;
    }

    /**
     * Lista planos de forma legível para debug
     */
    public void logAllPlans() {
        List<Map<String, Object>> plans = getPlans();

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 PLANOS NO MERCADO PAGO (Total: {})", plans.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (int i = 0; i < plans.size(); i++) {
            Map<String, Object> plan = plans.get(i);

            log.info("{}. {}", (i + 1), plan.get("name"));
            log.info("   ID: {}", plan.get("id"));
            log.info("   Status: {}", plan.get("status"));
            log.info("   Preço: {} {}/mês", plan.get("price"), plan.get("currency_id"));

            if (Boolean.TRUE.equals(plan.get("has_free_trial"))) {
                log.info("   🎁 Free Trial: {} {}",
                        plan.get("free_trial_frequency"),
                        plan.get("free_trial_frequency_type"));
            }

            log.info("");
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}