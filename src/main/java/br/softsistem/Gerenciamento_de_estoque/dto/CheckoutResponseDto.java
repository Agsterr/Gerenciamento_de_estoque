package br.softsistem.Gerenciamento_de_estoque.dto;

public class CheckoutResponseDto {
    private String initPoint;
    private String sessionId;
    private Long subscriptionId;
    private String status;
    private Boolean testMode;
    /** true quando o checkout foi feito com card_token (Checkout Transparente); não redirecionar */
    private Boolean transparentCheckout;
    /** ID da assinatura no Mercado Pago (preapproval_id) */
    private String preapprovalId;

    private String paymentUrl;
    private String paymentProvider;
    private String asaasPaymentId;
    private String paymentMode;
    private String billingType;
    private String pixQrCodeImage;
    private String pixCopyPaste;
    private String pixExpirationDate;
    private String bankSlipUrl;
    private String identificationField;
    private String dueDate;

    public CheckoutResponseDto() {}

    public CheckoutResponseDto(String initPoint, String sessionId) {
        this.initPoint = initPoint;
        this.sessionId = sessionId;
    }

    public CheckoutResponseDto(String initPoint, String sessionId, Long subscriptionId, String status) {
        this.initPoint = initPoint;
        this.sessionId = sessionId;
        this.subscriptionId = subscriptionId;
        this.status = status;
    }

    public CheckoutResponseDto(String initPoint, String sessionId, Long subscriptionId, String status, Boolean testMode) {
        this.initPoint = initPoint;
        this.sessionId = sessionId;
        this.subscriptionId = subscriptionId;
        this.status = status;
        this.testMode = testMode != null ? testMode : false;
    }

    public CheckoutResponseDto(String initPoint, String sessionId, Long subscriptionId, String status, Boolean testMode,
            Boolean transparentCheckout, String preapprovalId) {
        this.initPoint = initPoint;
        this.sessionId = sessionId;
        this.subscriptionId = subscriptionId;
        this.status = status;
        this.testMode = testMode != null ? testMode : false;
        this.transparentCheckout = Boolean.TRUE.equals(transparentCheckout);
        this.preapprovalId = preapprovalId;
    }

    public String getInitPoint() {
        return initPoint;
    }

    public void setInitPoint(String initPoint) {
        this.initPoint = initPoint;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getSubscriptionId() {
        return subscriptionId;
    }
    
    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getTestMode() {
        return testMode;
    }

    public void setTestMode(Boolean testMode) {
        this.testMode = testMode;
    }

    public Boolean getTransparentCheckout() {
        return transparentCheckout;
    }

    public void setTransparentCheckout(Boolean transparentCheckout) {
        this.transparentCheckout = transparentCheckout;
    }

    public String getPreapprovalId() {
        return preapprovalId;
    }

    public void setPreapprovalId(String preapprovalId) {
        this.preapprovalId = preapprovalId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getAsaasPaymentId() {
        return asaasPaymentId;
    }

    public void setAsaasPaymentId(String asaasPaymentId) {
        this.asaasPaymentId = asaasPaymentId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }

    public String getPixQrCodeImage() {
        return pixQrCodeImage;
    }

    public void setPixQrCodeImage(String pixQrCodeImage) {
        this.pixQrCodeImage = pixQrCodeImage;
    }

    public String getPixCopyPaste() {
        return pixCopyPaste;
    }

    public void setPixCopyPaste(String pixCopyPaste) {
        this.pixCopyPaste = pixCopyPaste;
    }

    public String getPixExpirationDate() {
        return pixExpirationDate;
    }

    public void setPixExpirationDate(String pixExpirationDate) {
        this.pixExpirationDate = pixExpirationDate;
    }

    public String getBankSlipUrl() {
        return bankSlipUrl;
    }

    public void setBankSlipUrl(String bankSlipUrl) {
        this.bankSlipUrl = bankSlipUrl;
    }

    public String getIdentificationField() {
        return identificationField;
    }

    public void setIdentificationField(String identificationField) {
        this.identificationField = identificationField;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}
