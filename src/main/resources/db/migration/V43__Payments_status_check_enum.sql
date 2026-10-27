-- O enum Payment.PaymentStatus (APPROVED, REJECTED, CANCELLED, CHARGED_BACK...) divergia do CHECK da V11,
-- fazendo o webhook do Asaas falhar ao gravar o pagamento e a assinatura nunca ativar.
-- Mantém os valores antigos e acrescenta os usados pelo código.

ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;

ALTER TABLE payments
    ADD CONSTRAINT payments_status_check CHECK (status IN (
        'PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'REFUNDED',
        'APPROVED', 'REJECTED', 'CANCELLED', 'CHARGED_BACK'
    ));
