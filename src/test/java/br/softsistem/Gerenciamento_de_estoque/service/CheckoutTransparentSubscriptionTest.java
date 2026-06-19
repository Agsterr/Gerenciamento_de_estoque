package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.AsaasConfig;
import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.config.PaymentProviderConfig;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes para o fluxo de Checkout Transparente (MPCP) e criação de assinatura com card_token_id.
 * Garante que o serviço chama a API do Mercado Pago com payer_email e card_token_id corretos.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutTransparentSubscriptionTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MercadoPagoConfig mercadoPagoConfig;

    @Mock
    private MercadoPagoService mercadoPagoService;

    @Mock
    private MercadoPagoPlanService mercadoPagoPlanService;

    @Mock
    private PaymentProviderConfig paymentProviderConfig;

    @Mock
    private AsaasService asaasService;

    @Mock
    private AsaasConfig asaasConfig;

    @Mock
    private TrialSubscriptionService trialSubscriptionService;

    private SubscriptionService subscriptionService;
    private Usuario usuario;
    private Plan plan;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                planRepository,
                usuarioRepository,
                paymentProviderConfig,
                asaasService,
                asaasConfig,
                mercadoPagoConfig,
                trialSubscriptionService,
                mercadoPagoService,
                mercadoPagoPlanService);
        when(paymentProviderConfig.isAsaas()).thenReturn(false);
        when(paymentProviderConfig.isMercadoPago()).thenReturn(true);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("payer@example.com");
        usuario.setUsername("payer");

        plan = new Plan();
        plan.setId(1L);
        plan.setName("Plano Básico");
        plan.setType(PlanType.BASIC);
        plan.setPrice(new BigDecimal("29.90"));
        plan.setMercadoPagoPreapprovalPlanId("2c938084726fca480172750000000000");
        plan.setIsActive(true);
    }

    @Test
    @DisplayName("createSubscriptionForUser com cardTokenId e payerEmail deve chamar createPreapproval com payer_email do pagador")
    void createSubscriptionForUser_comCardTokenEpayerEmail_chamaCreatePreapprovalComPayerEmail() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(planRepository.findByMercadoPagoPreapprovalPlanIdAndIsActiveTrue(anyString()))
                    .thenReturn(Optional.empty());
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any())).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> mpResponse = new HashMap<>();
            mpResponse.put("id", "preapproval-123");
            mpResponse.put("status", "pending");
            when(mercadoPagoService.createPreapproval(
                    eq(usuario),
                    eq(plan),
                    isNull(),
                    eq("card-token-xyz"),
                    eq("custom-payer@email.com")))
                    .thenReturn(mpResponse);

            Map<String, Object> result = subscriptionService.createSubscriptionForUser(
                    "1",
                    null,
                    "card-token-xyz",
                    "custom-payer@email.com");

            assertNotNull(result);
            assertTrue((Boolean) result.getOrDefault("transparentCheckout", false));
            assertEquals("preapproval-123", result.get("preapprovalId"));

            verify(mercadoPagoService).createPreapproval(
                    eq(usuario),
                    eq(plan),
                    isNull(),
                    eq("card-token-xyz"),
                    eq("custom-payer@email.com"));
        }
    }

    @Test
    @DisplayName("createSubscriptionForUser com payerEmail null envia email do usuário para createPreapproval")
    void createSubscriptionForUser_comPayerEmailNull_usaEmailDoUsuario() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(planRepository.findByMercadoPagoPreapprovalPlanIdAndIsActiveTrue(anyString()))
                    .thenReturn(Optional.empty());
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
            when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any())).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> mpResponse = new HashMap<>();
            mpResponse.put("id", "preapproval-456");
            when(mercadoPagoService.createPreapproval(
                    eq(usuario),
                    eq(plan),
                    isNull(),
                    eq("card-token-abc"),
                    eq("payer@example.com")))
                    .thenReturn(mpResponse);

            subscriptionService.createSubscriptionForUser("1", null, "card-token-abc", null);

            verify(mercadoPagoService).createPreapproval(
                    eq(usuario),
                    eq(plan),
                    isNull(),
                    eq("card-token-abc"),
                    eq("payer@example.com"));
        }
    }
}

