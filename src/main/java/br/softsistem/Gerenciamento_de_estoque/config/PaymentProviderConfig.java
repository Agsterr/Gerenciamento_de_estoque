package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderConfig {

    public static final String ASAAS = "asaas";
    public static final String MERCADOPAGO = "mercadopago";

    @Value("${app.payment.provider:asaas}")
    private String provider;

    public String getProvider() {
        return provider != null ? provider.toLowerCase() : ASAAS;
    }

    public boolean isAsaas() {
        return ASAAS.equals(getProvider());
    }

    public boolean isMercadoPago() {
        return MERCADOPAGO.equals(getProvider());
    }
}
