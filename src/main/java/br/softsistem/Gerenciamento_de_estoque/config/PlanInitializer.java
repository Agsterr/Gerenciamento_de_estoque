package br.softsistem.Gerenciamento_de_estoque.config;

import br.softsistem.Gerenciamento_de_estoque.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Componente responsável por inicializar e sincronizar planos com Stripe na inicialização da aplicação
 */
@Component
@Order(100) // Executa após outras inicializações
public class PlanInitializer implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(PlanInitializer.class);
    
    private final PlanService planService;
    private final StripeConfig stripeConfig;
    
    public PlanInitializer(PlanService planService, StripeConfig stripeConfig) {
        this.planService = planService;
        this.stripeConfig = stripeConfig;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Iniciando configuração de planos...");
        
        try {
            // Verificar se o Stripe está configurado
            if (!stripeConfig.isStripeConfigured()) {
                log.warn("Stripe não está configurado. Sincronização de planos será ignorada.");
                log.info("Para habilitar a sincronização automática, configure as chaves do Stripe no arquivo .env");
                return;
            }
            
            // Sincronizar planos com Stripe
            log.info("Stripe configurado. Iniciando sincronização de planos...");
            planService.syncAllPlansWithStripe();
            log.info("Sincronização de planos concluída com sucesso!");
            
        } catch (Exception e) {
            log.error("Erro durante a inicialização dos planos: {}", e.getMessage(), e);
            log.warn("A aplicação continuará funcionando, mas os planos podem não estar sincronizados com o Stripe");
        }
    }
}