package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service para envio de emails relacionados a assinaturas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.from-name}")
    private String fromName;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /**
     * Envia email de boas-vindas ao trial
     */
    public void sendTrialWelcomeEmail(Usuario user, Plan plan, int trialDays) {
        try {
            String subject = "Bem-vindo ao seu teste gratuito de " + trialDays + " dias!";
            String content = buildTrialWelcomeContent(user, plan, trialDays);
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Email de boas-vindas ao trial enviado para: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de boas-vindas ao trial para: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Envia email de alerta de fim de trial
     */
    public void sendTrialEndingEmail(Usuario user, Plan plan, LocalDateTime trialEndDate) {
        try {
            String subject = "Seu teste gratuito termina em breve!";
            String content = buildTrialEndingContent(user, plan, trialEndDate);
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Email de alerta de fim de trial enviado para: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de alerta de fim de trial para: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Envia email de trial expirado
     */
    public void sendTrialExpiredEmail(Usuario user, Plan plan) {
        try {
            String subject = "Seu teste gratuito expirou";
            String content = buildTrialExpiredContent(user, plan);
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Email de trial expirado enviado para: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de trial expirado para: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Envia email de conversão de trial para pago
     */
    public void sendTrialConvertedEmail(Usuario user, Plan plan) {
        try {
            String subject = "Assinatura ativada com sucesso!";
            String content = buildTrialConvertedContent(user, plan);
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Email de conversão de trial enviado para: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de conversão de trial para: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Envia email de assinatura cancelada
     */
    public void sendSubscriptionCanceledEmail(Usuario user, Plan plan) {
        try {
            String subject = "Assinatura cancelada";
            String content = buildSubscriptionCanceledContent(user, plan);
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Email de cancelamento enviado para: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de cancelamento para: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Envia email de falha de pagamento
     */
    public void sendPaymentFailedEmail(Usuario user, Plan plan, String reason) {
        try {
            String subject = "Falha no pagamento da sua assinatura";
            String content = buildPaymentFailedContent(user, plan, reason);
            
            sendEmail(user.getEmail(), subject, content);
            log.info("Email de falha de pagamento enviado para: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de falha de pagamento para: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Método genérico para envio de emails
     */
    private void sendEmail(String to, String subject, String content) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        
        mailSender.send(message);
    }
    
    // Métodos para construir o conteúdo dos emails
    
    private String buildTrialWelcomeContent(Usuario user, Plan plan, int trialDays) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Olá, %s!</h2>
                    
                    <p>Bem-vindo ao <strong>Sistema de Gerenciamento de Estoque</strong>!</p>
                    
                    <p>Seu teste gratuito de <strong>%d dias</strong> do plano <strong>%s</strong> foi ativado com sucesso.</p>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <h3 style="margin-top: 0; color: #495057;">O que você pode fazer durante o teste:</h3>
                        <ul>
                            <li>Gerenciar produtos e estoque</li>
                            <li>Controlar movimentações</li>
                            <li>Gerar relatórios básicos</li>
                            %s
                        </ul>
                    </div>
                    
                    <p>Aproveite ao máximo seu período de teste. Caso tenha dúvidas, nossa equipe está sempre disponível para ajudar.</p>
                    
                    <p style="margin-top: 30px;">Atenciosamente,<br>
                    <strong>Equipe Sistema de Gerenciamento de Estoque</strong></p>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(), 
            trialDays, 
            plan.getName(),
            buildPlanFeatures(plan)
        );
    }
    
    private String buildTrialEndingContent(Usuario user, Plan plan, LocalDateTime trialEndDate) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #e74c3c;">Seu teste gratuito termina em breve!</h2>
                    
                    <p>Olá, %s!</p>
                    
                    <p>Seu teste gratuito do plano <strong>%s</strong> terminará em <strong>%s</strong>.</p>
                    
                    <div style="background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ffc107;">
                        <p style="margin: 0;"><strong>Não perca o acesso!</strong> Para continuar usando todas as funcionalidades, ative sua assinatura antes do vencimento.</p>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="#" style="background-color: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Ativar Assinatura</a>
                    </div>
                    
                    <p>Caso tenha dúvidas, entre em contato conosco.</p>
                    
                    <p style="margin-top: 30px;">Atenciosamente,<br>
                    <strong>Equipe Sistema de Gerenciamento de Estoque</strong></p>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(), 
            plan.getName(),
            trialEndDate.format(DATE_FORMATTER)
        );
    }
    
    private String buildTrialExpiredContent(Usuario user, Plan plan) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #dc3545;">Seu teste gratuito expirou</h2>
                    
                    <p>Olá, %s!</p>
                    
                    <p>Seu teste gratuito do plano <strong>%s</strong> expirou.</p>
                    
                    <p>Para continuar aproveitando todas as funcionalidades do sistema, ative sua assinatura agora mesmo.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="#" style="background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Escolher Plano</a>
                    </div>
                    
                    <p>Obrigado por experimentar nosso sistema!</p>
                    
                    <p style="margin-top: 30px;">Atenciosamente,<br>
                    <strong>Equipe Sistema de Gerenciamento de Estoque</strong></p>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(), 
            plan.getName()
        );
    }
    
    private String buildTrialConvertedContent(Usuario user, Plan plan) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #28a745;">Assinatura ativada com sucesso!</h2>
                    
                    <p>Olá, %s!</p>
                    
                    <p>Parabéns! Sua assinatura do plano <strong>%s</strong> foi ativada com sucesso.</p>
                    
                    <p>Agora você tem acesso completo a todas as funcionalidades do sistema sem limitações.</p>
                    
                    <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #28a745;">
                        <p style="margin: 0;"><strong>Próxima cobrança:</strong> Sua próxima fatura será processada automaticamente no próximo mês.</p>
                    </div>
                    
                    <p>Obrigado por escolher nosso sistema!</p>
                    
                    <p style="margin-top: 30px;">Atenciosamente,<br>
                    <strong>Equipe Sistema de Gerenciamento de Estoque</strong></p>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(), 
            plan.getName()
        );
    }
    
    private String buildSubscriptionCanceledContent(Usuario user, Plan plan) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #6c757d;">Assinatura cancelada</h2>
                    
                    <p>Olá, %s!</p>
                    
                    <p>Sua assinatura do plano <strong>%s</strong> foi cancelada conforme solicitado.</p>
                    
                    <p>Você continuará tendo acesso ao sistema até o final do período já pago.</p>
                    
                    <p>Sentiremos sua falta! Se mudar de ideia, você pode reativar sua assinatura a qualquer momento.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="#" style="background-color: #6c757d; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Reativar Assinatura</a>
                    </div>
                    
                    <p style="margin-top: 30px;">Atenciosamente,<br>
                    <strong>Equipe Sistema de Gerenciamento de Estoque</strong></p>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(), 
            plan.getName()
        );
    }
    
    private String buildPaymentFailedContent(Usuario user, Plan plan, String reason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #dc3545;">Falha no pagamento da sua assinatura</h2>
                    
                    <p>Olá, %s!</p>
                    
                    <p>Não conseguimos processar o pagamento da sua assinatura do plano <strong>%s</strong>.</p>
                    
                    <div style="background-color: #f8d7da; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #dc3545;">
                        <p style="margin: 0;"><strong>Motivo:</strong> %s</p>
                    </div>
                    
                    <p>Para evitar a suspensão do seu acesso, atualize suas informações de pagamento o quanto antes.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="#" style="background-color: #dc3545; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Atualizar Pagamento</a>
                    </div>
                    
                    <p style="margin-top: 30px;">Atenciosamente,<br>
                    <strong>Equipe Sistema de Gerenciamento de Estoque</strong></p>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(), 
            plan.getName(),
            reason != null ? reason : "Informações de pagamento inválidas"
        );
    }
    
    private String buildPlanFeatures(Plan plan) {
        StringBuilder features = new StringBuilder();
        
        if (plan.getHasReports()) {
            features.append("<li>Relatórios avançados</li>");
        }
        if (plan.getHasAdvancedAnalytics()) {
            features.append("<li>Analytics avançados</li>");
        }
        if (plan.getHasApiAccess()) {
            features.append("<li>Acesso à API</li>");
        }
        
        return features.toString();
    }
}