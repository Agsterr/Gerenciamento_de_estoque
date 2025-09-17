package br.softsistem.Gerenciamento_de_estoque.service;

// Removidos imports Lombok
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service para tarefas agendadas relacionadas a assinaturas
 */
@Service
public class SubscriptionScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);
    
    private final SubscriptionService subscriptionService;
    
    // Construtor explícito para injeção do SubscriptionService
    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }
    
    /**
     * Verifica trials que estão próximos do fim e envia alertas
     * Executa a cada 6 horas
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 horas em milissegundos
    public void checkTrialsEndingSoon() {
        try {
            log.info("Iniciando verificação de trials próximos do fim");
            subscriptionService.sendTrialEndingAlerts();
            log.info("Verificação de trials próximos do fim concluída");
        } catch (Exception e) {
            log.error("Erro ao verificar trials próximos do fim", e);
        }
    }
    
    /**
     * Processa trials expirados
     * Executa diariamente às 02:00
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void processExpiredTrials() {
        try {
            log.info("Iniciando processamento de trials expirados");
            subscriptionService.processExpiredTrials();
            log.info("Processamento de trials expirados concluído");
        } catch (Exception e) {
            log.error("Erro ao processar trials expirados", e);
        }
    }
    
    /**
     * Limpeza de dados antigos (opcional)
     * Executa semanalmente aos domingos às 03:00
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void weeklyCleanup() {
        try {
            log.info("Iniciando limpeza semanal de dados");
            // Aqui você pode implementar limpeza de dados antigos se necessário
            // Por exemplo: remover logs antigos, arquivar assinaturas antigas, etc.
            log.info("Limpeza semanal concluída");
        } catch (Exception e) {
            log.error("Erro na limpeza semanal", e);
        }
    }
    
    /**
     * Relatório diário de assinaturas (opcional)
     * Executa diariamente às 08:00
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailySubscriptionReport() {
        try {
            log.info("Gerando relatório diário de assinaturas");
            // Aqui você pode implementar um relatório diário
            // Por exemplo: contar assinaturas ativas, trials, cancelamentos, etc.
            log.info("Relatório diário de assinaturas gerado");
        } catch (Exception e) {
            log.error("Erro ao gerar relatório diário", e);
        }
    }
}