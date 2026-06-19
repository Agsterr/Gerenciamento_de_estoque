import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

declare const MercadoPago: any;

@Injectable({ providedIn: 'root' })
export class MercadoPagoService {
  private mpInstance: any = null;

  constructor(private http: HttpClient) {}

  getPublicKey(): Observable<{ publicKey: string }> {
    return this.http.get<{ publicKey: string }>(`${environment.apiUrl}/api/mercadopago/public-key`);
  }

  async init(): Promise<void> {
    if (this.mpInstance) return;
    const { publicKey } = await firstValueFrom(this.getPublicKey());
    if (!publicKey) throw new Error('Public Key do Mercado Pago não configurada');
    await this.loadScript();
    this.mpInstance = new MercadoPago(publicKey);
  }

  async createCardToken(cardData: {
    cardNumber: string; cardholderName: string; cardExpirationMonth: string;
    cardExpirationYear: string; securityCode: string; identificationType: string; identificationNumber: string;
  }): Promise<string> {
    await this.init();
    const result = await this.mpInstance.createCardToken({
      cardNumber: cardData.cardNumber.replace(/\s/g, ''),
      cardholderName: cardData.cardholderName,
      cardExpirationMonth: cardData.cardExpirationMonth,
      cardExpirationYear: cardData.cardExpirationYear,
      securityCode: cardData.securityCode,
      identificationType: cardData.identificationType,
      identificationNumber: cardData.identificationNumber,
    });
    if (!result?.id) throw new Error('Falha ao gerar token do cartão');
    return result.id;
  }

  private loadScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (document.getElementById('mp-sdk')) { resolve(); return; }
      const script = document.createElement('script');
      script.id = 'mp-sdk';
      script.src = 'https://sdk.mercadopago.com/js/v2';
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Falha ao carregar MercadoPago.js'));
      document.body.appendChild(script);
    });
  }
}
