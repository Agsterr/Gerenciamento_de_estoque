package br.softsistem.Gerenciamento_de_estoque.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.softsistem.Gerenciamento_de_estoque.service.PlanService;

/**
 * Inicializa planos na startup. Com Asaas usa planos locais; com Mercado Pago sincroniza da API.
 */
@Component
@Order(100)
public class PlanInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanInitializer.class);

    private final PlanService planService;
    private final PaymentProviderConfig paymentProviderConfig;
    private final MercadoPagoConfig mercadoPagoConfig;

    public PlanInitializer(
            PlanService planService,
            PaymentProviderConfig paymentProviderConfig,
            MercadoPagoConfig mercadoPagoConfig) {
        this.planService = planService;
        this.paymentProviderConfig = paymentProviderConfig;
        this.mercadoPagoConfig = mercadoPagoConfig;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Iniciando configuração de planos (provedor: {})...", paymentProviderConfig.getProvider());

        if (paymentProviderConfig.isMercadoPago()) {
            syncMercadoPagoPlans();
        } else {
            planService.getDefaultSaasPlan().ifPresentOrElse(
                    plan -> log.info("Plano SaaS ativo: {} — R$ {}/mês (checkout envia valor ao Asaas)",
                            plan.getName(), plan.getPrice()),
                    () -> log.warn("Nenhum plano SaaS ativo encontrado. Execute as migrations ou crie o plano BASIC."));
        }
    }

    private void syncMercadoPagoPlans() {
        try {
            if (!mercadoPagoConfig.isMercadoPagoConfigured()) {
                log.warn("Mercado Pago não configurado. Sincronização ignorada.");
                return;
            }
            log.info("Sincronizando planos do Mercado Pago...");
            var result = planService.syncMercadoPagoPlans(true);
            log.info("Sincronização concluída: {}", result);
        } catch (Exception e) {
            log.error("Erro na sincronização de planos MP: {}", e.getMessage(), e);
        }
    }
}
