package br.softsistem.Gerenciamento_de_estoque.integration;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import br.softsistem.Gerenciamento_de_estoque.service.WebhookSignatureValidator;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoSandboxIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "MERCADOPAGO_TEST_ACCESS_TOKEN", matches = ".+")
    void shouldCreatePreferenceInSandbox() throws Exception {
        Assumptions.assumeTrue(
                "true".equalsIgnoreCase(System.getenv("RUN_MP_SANDBOX_TESTS")),
                "Teste sandbox desabilitado. Defina RUN_MP_SANDBOX_TESTS=true para executar.");
        String token = System.getenv("MERCADOPAGO_TEST_ACCESS_TOKEN");
        assertNotNull(token);
        MercadoPagoConfig.setAccessToken(token);

        String base = System.getenv("APP_PUBLIC_URL");
        if (base == null || base.isBlank()) {
            base = "https://gerenciamento-de-estoque-front.vercel.app";
        }
        String success = envOrDefault("MERCADOPAGO_SUCCESS_URL", base + "/subscription/success");
        String failure = envOrDefault("MERCADOPAGO_CANCEL_URL", base + "/subscription/cancel");
        String pending = envOrDefault("MERCADOPAGO_PENDING_URL", base + "/subscription/pending");

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Plano Teste")
                .description("Integração Sandbox")
                .quantity(1)
                .unitPrice(new BigDecimal("1.00"))
                .currencyId("BRL")
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(java.util.List.of(item))
                .payer(PreferencePayerRequest.builder()
                        .email("sandbox-user@example.com")
                        .name("Sandbox User")
                        .build())
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(success)
                        .failure(failure)
                        .pending(pending)
                        .build())
                .notificationUrl(base.endsWith("/") ? base.substring(0, base.length() - 1) + "/api/webhooks/mercadopago" : base + "/api/webhooks/mercadopago")
                .externalReference("integration_test_" + System.currentTimeMillis())
                .autoReturn("approved")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference pref = client.create(request);
        assertNotNull(pref);
        assertNotNull(pref.getId());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MERCADOPAGO_WEBHOOK_SECRET", matches = ".+")
    void shouldValidateWebhookSignature() throws Exception {
        Assumptions.assumeTrue(
                "true".equalsIgnoreCase(System.getenv("RUN_MP_SANDBOX_TESTS")),
                "Teste sandbox desabilitado. Defina RUN_MP_SANDBOX_TESTS=true para executar.");
        String secret = System.getenv("MERCADOPAGO_WEBHOOK_SECRET");
        assertNotNull(secret);
        String body = "{\"id\":\"evt_123\",\"type\":\"payment\",\"data\":{\"id\":\"7890\"}}";
        long ts = System.currentTimeMillis() / 1000;
        String signedPayload = ts + "." + body;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hashBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        String v1 = bytesToHex(hashBytes);
        String signature = "ts=" + ts + ",v1=" + v1;

        WebhookSignatureValidator validator = new WebhookSignatureValidator();
        assertTrue(validator.validate(signature, body, secret));
    }

    private String envOrDefault(String name, String def) {
        String v = System.getenv(name);
        return v != null && !v.isBlank() ? v : def;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

