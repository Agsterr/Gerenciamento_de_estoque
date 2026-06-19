package br.softsistem.Gerenciamento_de_estoque.config;



import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;



import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;



/**

 * Configuração do Asaas (sandbox e produção).

 * Chaves distintas por ambiente conforme documentação oficial.

 */

@Component

public class AsaasConfig {



    private static final Logger log = LoggerFactory.getLogger(AsaasConfig.class);



    public static final String SANDBOX_BASE_URL = "https://api-sandbox.asaas.com/v3";

    public static final String PRODUCTION_BASE_URL = "https://api.asaas.com/v3";



    @Value("${asaas.environment:sandbox}")

    private String environment;



    @Value("${asaas.sandbox.api-key:}")

    private String sandboxApiKey;



    @Value("${asaas.sandbox.api-key-file:}")

    private String sandboxApiKeyFile;



    @Value("${asaas.prod.api-key:}")

    private String prodApiKey;



    @Value("${asaas.prod.api-key-file:}")

    private String prodApiKeyFile;



    @Value("${asaas.webhook.token:}")

    private String webhookToken;



    @Value("${app.public-url:}")

    private String appPublicUrl;



    /** Redirecionamento pós-pagamento (callback.successUrl). Requer domínio cadastrado no painel Asaas. */
    @Value("${asaas.payment.callback-enabled:false}")

    private boolean paymentCallbackEnabled;



    public boolean isSandbox() {

        return !"production".equalsIgnoreCase(environment);

    }



    public boolean isProduction() {

        return "production".equalsIgnoreCase(environment);

    }



    public String getApiKey() {

        String fromFile = readKeyFromFile(isSandbox() ? sandboxApiKeyFile : prodApiKeyFile);

        if (fromFile != null && !fromFile.isBlank()) {

            return normalizeApiKey(fromFile);

        }

        String raw = isSandbox() ? sandboxApiKey : prodApiKey;

        return normalizeApiKey(raw);

    }



    public String getBaseUrl() {

        return isSandbox() ? SANDBOX_BASE_URL : PRODUCTION_BASE_URL;

    }



    public String getWebhookToken() {

        return webhookToken;

    }



    public String getAppPublicUrl() {

        return appPublicUrl;

    }



    public boolean isPaymentCallbackEnabled() {

        return paymentCallbackEnabled;

    }



    public boolean isConfigured() {

        String key = getApiKey();

        return key != null && !key.isBlank() && key.startsWith("$aact_");

    }



    /** Remove aspas acidentais e normaliza chaves lidas do .env / Docker. */

    public String normalizeApiKey(String key) {

        if (key == null) {

            return null;

        }

        String trimmed = key.trim();

        if ((trimmed.startsWith("'") && trimmed.endsWith("'")) || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {

            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();

        }

        return trimmed;

    }



    private String readKeyFromFile(String filePath) {

        if (filePath == null || filePath.isBlank()) {

            return null;

        }

        try {

            Path path = Path.of(filePath.trim());

            if (!Files.isRegularFile(path)) {

                return null;

            }

            String content = Files.readString(path).trim();

            if (content.startsWith("#")) {

                return null;

            }

            return content.lines()

                    .map(String::trim)

                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))

                    .findFirst()

                    .orElse(null);

        } catch (IOException e) {

            log.warn("Não foi possível ler chave Asaas em {}: {}", filePath, e.getMessage());

            return null;

        }

    }



    public String getWebhookUrl() {

        if (appPublicUrl == null || appPublicUrl.isBlank()) {

            return null;

        }

        String base = appPublicUrl.endsWith("/") ? appPublicUrl.substring(0, appPublicUrl.length() - 1) : appPublicUrl;

        return base + "/api/webhooks/asaas";

    }

}


