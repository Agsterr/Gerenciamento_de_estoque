export interface Subscription {
  id: number;
  userId: number;
  userEmail?: string;
  userName?: string;
  planId: number;
  planName?: string;
  planType?: string;
  planPrice?: number;
  status: string;
  trialStart?: string;
  trialEnd?: string;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  canceledAt?: string;
  endedAt?: string;
  createdAt?: string;
  isInTrial?: boolean;
  isTrialEndingSoon?: boolean;
  isActive?: boolean;
  maxUsers?: number;
  maxProducts?: number;
  preapprovalId?: string;
  trialDaysRemaining?: number;
  trialDaysElapsed?: number;
  trialDaysTotal?: number;
  paymentUrl?: string;
  paymentProvider?: string;
  asaasPaymentId?: string;
  paymentMode?: string;
  pendingPayment?: boolean;
}

export type AsaasPaymentMode = 'RECURRING' | 'PIX' | 'BOLETO';

export interface CheckoutResponse {
  initPoint?: string;
  sessionId?: string;
  subscriptionId?: number;
  status?: string;
  testMode?: boolean;
  transparentCheckout?: boolean;
  preapprovalId?: string;
  paymentUrl?: string;
  paymentProvider?: string;
  asaasPaymentId?: string;
  paymentMode?: AsaasPaymentMode;
  billingType?: string;
  pixQrCodeImage?: string;
  pixCopyPaste?: string;
  pixExpirationDate?: string;
  bankSlipUrl?: string;
  identificationField?: string;
  dueDate?: string;
}
