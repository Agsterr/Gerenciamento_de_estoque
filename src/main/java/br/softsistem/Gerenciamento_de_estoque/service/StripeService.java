package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.StripeConfig;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service para integração com a API do Stripe
 */
@Service
public class StripeService {
    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final StripeConfig stripeConfig;

    public StripeService(StripeConfig stripeConfig) {
        this.stripeConfig = stripeConfig;
    }

    /**
     * Cria um cliente no Stripe
     */
    public Customer createCustomer(Usuario user) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getUsername())
                .putMetadata("user_id", user.getId().toString())
                .build();
        
        Customer customer = Customer.create(params);
        log.info("Cliente Stripe criado: {} para usuário: {}", customer.getId(), user.getId());
        return customer;
    }
    
    /**
     * Cria um produto no Stripe
     */
    public Product createProduct(Plan plan) throws StripeException {
        ProductCreateParams params = ProductCreateParams.builder()
                .setName(plan.getName())
                .setDescription(plan.getDescription())
                .putMetadata("plan_id", plan.getId().toString())
                .build();
        
        Product product = Product.create(params);
        log.info("Produto Stripe criado: {} para plano: {}", product.getId(), plan.getId());
        return product;
    }
    
    /**
     * Cria um preço no Stripe
     */
    public Price createPrice(Plan plan, String productId) throws StripeException {
        // Converte o preço para centavos
        long unitAmount = plan.getPrice().multiply(BigDecimal.valueOf(100)).longValue();
        
        PriceCreateParams params = PriceCreateParams.builder()
                .setProduct(productId)
                .setUnitAmount(unitAmount)
                .setCurrency("brl")
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build()
                )
                .putMetadata("plan_id", plan.getId().toString())
                .build();
        
        Price price = Price.create(params);
        log.info("Preço Stripe criado: {} para plano: {}", price.getId(), plan.getId());
        return price;
    }
    
    /**
     * Cria uma sessão de checkout para trial
     */
    public Session createTrialCheckoutSession(Usuario user, Plan plan, String customerId) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(stripeConfig.getCancelUrl())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L)
                        .build()
                )
                .setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                        .setTrialPeriodDays(14L) // 14 dias de trial
                        .putMetadata("user_id", user.getId().toString())
                        .putMetadata("plan_id", plan.getId().toString())
                        .build()
                )
                .build();
        
        Session session = Session.create(params);
        log.info("Sessão de checkout trial criada: {} para usuário: {}", session.getId(), user.getId());
        return session;
    }
    
    /**
     * Cria uma sessão de checkout para pagamento imediato
     */
    public Session createPaymentCheckoutSession(Usuario user, Plan plan, String customerId) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(stripeConfig.getCancelUrl())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L)
                        .build()
                )
                .setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("user_id", user.getId().toString())
                        .putMetadata("plan_id", plan.getId().toString())
                        .build()
                )
                .build();
        
        Session session = Session.create(params);
        log.info("Sessão de checkout pagamento criada: {} para usuário: {}", session.getId(), user.getId());
        return session;
    }
    
    /**
     * Recupera uma assinatura do Stripe
     */
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }
    
    /**
     * Cancela uma assinatura no Stripe
     */
    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);
        SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                .setInvoiceNow(false)
                .setProrate(false)
                .build();
        
        Subscription canceledSubscription = subscription.cancel(params);
        log.info("Assinatura cancelada no Stripe: {}", subscriptionId);
        return canceledSubscription;
    }
    
    /**
     * Atualiza uma assinatura no Stripe
     */
    public Subscription updateSubscription(String subscriptionId, String newPriceId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);
        
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(subscription.getItems().getData().get(0).getId())
                        .setPrice(newPriceId)
                        .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();
        
        Subscription updatedSubscription = subscription.update(params);
        log.info("Assinatura atualizada no Stripe: {}", subscriptionId);
        return updatedSubscription;
    }
    
    /**
     * Cria um portal de cobrança para o cliente
     */
    public com.stripe.model.billingportal.Session createBillingPortalSession(String customerId, String returnUrl) throws StripeException {
        com.stripe.param.billingportal.SessionCreateParams params = 
            com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(returnUrl)
                .build();
        
        com.stripe.model.billingportal.Session session = com.stripe.model.billingportal.Session.create(params);
        log.info("Portal de cobrança criado para cliente: {}", customerId);
        return session;
    }
    
    /**
     * Valida um webhook do Stripe
     */
    public Event validateWebhook(String payload, String sigHeader) throws StripeException {
        return com.stripe.net.Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    }
}