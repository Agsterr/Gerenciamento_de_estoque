# 📘 GUIA FRONT-END ANGULAR - ASSINATURAS RECORRENTES MERCADO PAGO

> **Documento técnico para desenvolvedores Angular**  
> **Última atualização:** 2025-01-15  
> **Versão:** 2.0 - Assinaturas Recorrentes Corrigidas

---

## 📋 ÍNDICE

1. [O Que Mudou no Back-end](#1-o-que-mudou-no-back-end)
2. [Impacto no Front-end](#2-impacto-no-front-end)
3. [Painel de Criação de Planos](#3-painel-de-criação-de-planos)
4. [Implementação no Angular](#4-implementação-no-angular)
5. [Checklist de Verificação](#5-checklist-de-verificação)
6. [Exemplos de Código](#6-exemplos-de-código)

---

## 1️⃣ O QUE MUDOU NO BACK-END

### ⚠️ **MUDANÇA CRÍTICA: API Corrigida para Assinaturas Recorrentes**

O back-end foi **corrigido** para usar a API correta do Mercado Pago:

#### ❌ **ANTES (INCORRETO):**
- Usava `Preference` (`/preference`) - **Pagamento único**
- Criava apenas uma preferência de pagamento
- **NÃO era uma assinatura recorrente real**

#### ✅ **AGORA (CORRETO):**
- Usa `PreapprovalPlan` (`/preapproval_plan`) - **Plano de assinatura recorrente**
- Usa `Preapproval` (`/preapproval`) - **Assinatura do cliente**
- **É uma assinatura recorrente real** que cobra mensalmente automaticamente

### 🔄 **Fluxo Correto Agora:**

```
1. Usuário seleciona plano no front-end
2. Front-end chama: POST /api/subscription/create?planId={id}
3. Back-end cria PreapprovalPlan no Mercado Pago (plano de recorrência)
4. Back-end cria Preapproval no Mercado Pago (assinatura do cliente)
5. Back-end retorna URL de checkout
6. Usuário completa pagamento no Mercado Pago
7. Mercado Pago cobra automaticamente todo mês
```

---

## 2️⃣ IMPACTO NO FRONT-END

### ✅ **BOA NOTÍCIA: Endpoints NÃO Mudaram!**

Os endpoints da API **permanecem os mesmos**. O front-end **NÃO precisa mudar** a forma como chama a API.

### 📌 **Endpoints que Continuam Funcionando:**

| Endpoint | Método | Descrição | Mudou? |
|----------|--------|-----------|--------|
| `/api/subscription/create?planId={id}` | POST | Cria assinatura | ❌ **NÃO** |
| `/api/subscription/current` | GET | Consulta assinatura atual | ❌ **NÃO** |
| `/api/subscription/cancel` | POST | Cancela assinatura | ❌ **NÃO** |
| `/api/subscription/customer-portal` | GET | Portal do cliente | ❌ **NÃO** |
| `/api/subscription/history` | GET | Histórico de assinaturas | ❌ **NÃO** |
| `/api/plans` | GET | Lista planos | ❌ **NÃO** |
| `/api/plans/{id}` | GET | Detalhes do plano | ❌ **NÃO** |

### ⚠️ **O QUE MUDOU (Internamente no Back-end):**

- **Internamente**, o back-end agora usa a API correta do Mercado Pago
- **A resposta** continua no mesmo formato
- **O comportamento** é o mesmo para o front-end
- **A diferença** é que agora é uma assinatura recorrente REAL

---

## 3️⃣ PAINEL DE CRIAÇÃO DE PLANOS

### 📋 **Status Atual do Painel:**

Se você tem um painel no front-end que **cria planos**, ele está funcionando corretamente se:

1. ✅ Usa o endpoint `GET /api/plans` para listar planos
2. ✅ Usa o endpoint `GET /api/plans/{id}` para detalhes
3. ✅ **NÃO** precisa criar planos no Mercado Pago diretamente
4. ✅ Os planos são criados no banco de dados do sistema
5. ✅ A sincronização com Mercado Pago é feita pelo back-end

### 🔍 **Verificação Necessária:**

#### ✅ **O QUE ESTÁ CORRETO:**

- **Criar planos no banco de dados** via API `POST /api/plans` ✅ **AGORA EXISTE**
- **Listar planos** do banco de dados via `GET /api/plans`
- **Exibir planos** para o usuário escolher
- **Criar assinatura** usando `POST /api/subscription/create?planId={id}`

#### ⚠️ **O QUE NÃO DEVE FAZER:**

- ❌ **NÃO** criar planos diretamente no Mercado Pago
- ❌ **NÃO** chamar APIs do Mercado Pago diretamente do front-end
- ❌ **NÃO** usar Access Token do Mercado Pago no front-end
- ❌ **NÃO** criar `PreapprovalPlan` manualmente

### 📝 **Criar Plano (POST /api/plans)**

**✅ AGORA EXISTE!** O endpoint para criar planos foi implementado.

**Método:** `POST`  
**Endpoint:** `/api/plans`  
**Autenticação:** Requerida (Role: ADMIN)  
**Content-Type:** `application/json`

**Body da Requisição (JSON):**
```json
{
  "name": "Nome do Plano",
  "description": "Descrição do plano",
  "price": 99.90,
  "type": "PROFESSIONAL",
  "maxUsers": 10,
  "maxProducts": 500,
  "maxOrganizations": null,  // ⚠️ Opcional - pode ser null
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true
}
```

**Campos Obrigatórios:**
- `name` (string) - Nome do plano
- `price` (number) - Preço em reais (deve ser > 0)
- `type` (string) - Tipo: `BASIC`, `PROFESSIONAL` ou `ENTERPRISE`

**Campos Opcionais:**
- `description` (string) - Descrição do plano
- `maxUsers` (number) - Limite de usuários (null = ilimitado)
- `maxProducts` (number) - Limite de produtos (null = ilimitado)
- `maxOrganizations` (number) - ⚠️ **OPCIONAL** - Limite de organizações. **NOTA:** No modelo atual (1 usuário = 1 organização), este campo pode não fazer sentido. Recomenda-se sempre usar `null`.
- `hasReports` (boolean) - Acesso a relatórios (padrão: false)
- `hasAdvancedAnalytics` (boolean) - Analytics avançado (padrão: false)
- `hasApiAccess` (boolean) - Acesso à API (padrão: false)
- `isActive` (boolean) - Plano ativo (padrão: true)

**Resposta (201 Created):**
```json
{
  "id": 1,
  "name": "Nome do Plano",
  "description": "Descrição do plano",
  "price": 99.90,
  "type": "PROFESSIONAL",
  "maxUsers": 10,
  "maxProducts": 500,
  "maxOrganizations": null,  // ⚠️ Opcional - pode ser null
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true,
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

**Possíveis Erros:**
- `400 Bad Request` - Dados inválidos (nome vazio, preço <= 0, etc.)
- `401 Unauthorized` - Token inválido ou sem permissão ADMIN
- `409 Conflict` - Já existe plano com este nome

**Exemplo Angular:**
```typescript
createPlan(planData: any): Observable<Plan> {
  const headers = this.getAuthHeaders();
  return this.http.post<Plan>(
    `${this.apiUrl}/api/plans`,
    planData,
    { headers }
  );
}
```

### 📝 **Sobre Sincronização com Mercado Pago:**

O back-end tem um endpoint para sincronizar planos:

```
POST /api/plans/sync-mercadopago
```

**IMPORTANTE:**
- Este endpoint **NÃO cria planos no banco de dados**
- Ele apenas **valida** que os planos existentes estão prontos para o Mercado Pago
- Os planos são criados **dinamicamente** quando o usuário faz a assinatura
- **NÃO é necessário** chamar este endpoint antes de criar assinaturas

---

## 4️⃣ IMPLEMENTAÇÃO NO ANGULAR

### 📦 **1. Service de Assinaturas (SubscriptionService)**

Se você **já tem** um `SubscriptionService`, ele deve estar assim:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private apiUrl = `${environment.apiUrl}/api/subscription`;

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('Token não encontrado. Faça login.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  /**
   * Cria uma nova assinatura recorrente
   * AGORA usa PreapprovalPlan (correto para assinaturas recorrentes)
   */
  createSubscription(planId: number): Observable<{ checkoutUrl: string; sessionId: string }> {
    const headers = this.getAuthHeaders();
    return this.http.post<{ checkoutUrl: string; sessionId: string }>(
      `${this.apiUrl}/create?planId=${planId}`,
      {},
      { headers }
    );
  }

  /**
   * Consulta assinatura atual do usuário
   */
  getCurrentSubscription(): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.get<any>(`${this.apiUrl}/current`, { headers });
  }

  /**
   * Cancela assinatura atual
   */
  cancelSubscription(): Observable<{ message: string }> {
    const headers = this.getAuthHeaders();
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/cancel`,
      {},
      { headers }
    );
  }

  /**
   * Obtém URL do portal do cliente
   */
  getCustomerPortal(): Observable<{ url: string }> {
    const headers = this.getAuthHeaders();
    return this.http.get<{ url: string }>(`${this.apiUrl}/customer-portal`, { headers });
  }

  /**
   * Obtém histórico de assinaturas
   */
  getSubscriptionHistory(): Observable<any[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<any[]>(`${this.apiUrl}/history`, { headers });
  }
}
```

### 🎯 **2. Componente de Seleção de Plano**

Exemplo de componente que cria assinatura:

```typescript
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlanService } from '../services/plan.service';
import { SubscriptionService } from '../services/subscription.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-plan-selection',
  templateUrl: './plan-selection.component.html'
})
export class PlanSelectionComponent implements OnInit {
  plans: any[] = [];
  isLoading = false;
  selectedPlanId: number | null = null;

  constructor(
    private planService: PlanService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.isLoading = true;
    this.planService.getAllPlans().subscribe({
      next: (plans) => {
        this.plans = plans;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar planos:', error);
        this.snackBar.open('Erro ao carregar planos', 'Fechar', { duration: 5000 });
        this.isLoading = false;
      }
    });
  }

  /**
   * Cria assinatura recorrente usando PreapprovalPlan
   * O back-end agora usa a API correta automaticamente
   */
  subscribeToPlan(planId: number): void {
    if (this.isLoading) return;

    this.isLoading = true;
    this.selectedPlanId = planId;

    this.subscriptionService.createSubscription(planId).subscribe({
      next: (response) => {
        // Redireciona para o checkout do Mercado Pago
        // A URL já vem pronta do back-end
        if (response.checkoutUrl) {
          window.location.href = response.checkoutUrl;
        } else {
          this.snackBar.open('Erro: URL de checkout não retornada', 'Fechar', { duration: 5000 });
          this.isLoading = false;
        }
      },
      error: (error) => {
        console.error('Erro ao criar assinatura:', error);
        
        let errorMessage = 'Erro ao criar assinatura. Tente novamente.';
        
        if (error.status === 400) {
          errorMessage = error.error?.message || 'Usuário já possui uma assinatura ativa';
        } else if (error.status === 401) {
          errorMessage = 'Sessão expirada. Faça login novamente.';
          // Redirecionar para login
          this.router.navigate(['/login']);
        } else if (error.status === 404) {
          errorMessage = 'Plano não encontrado';
        } else if (error.status === 500) {
          errorMessage = 'Erro no servidor. Tente novamente mais tarde.';
        }

        this.snackBar.open(errorMessage, 'Fechar', { duration: 5000 });
        this.isLoading = false;
        this.selectedPlanId = null;
      }
    });
  }
}
```

### 🔄 **3. Tratamento de Retorno do Checkout**

Após o usuário completar o pagamento no Mercado Pago, ele será redirecionado para as URLs configuradas:

- **Sucesso:** `mercadopago.success.url` (configurado no back-end)
- **Cancelado:** `mercadopago.cancel.url`
- **Pendente:** `mercadopago.pending.url`

**Exemplo de componente de retorno:**

```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SubscriptionService } from '../services/subscription.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-subscription-success',
  templateUrl: './subscription-success.component.html'
})
export class SubscriptionSuccessComponent implements OnInit {
  isLoading = true;
  subscription: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private subscriptionService: SubscriptionService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Aguardar alguns segundos para o webhook processar
    setTimeout(() => {
      this.loadSubscription();
    }, 2000);
  }

  loadSubscription(): void {
    this.subscriptionService.getCurrentSubscription().subscribe({
      next: (subscription) => {
        this.subscription = subscription;
        this.isLoading = false;
        
        if (subscription.status === 'ACTIVE' || subscription.status === 'TRIAL') {
          this.snackBar.open('Assinatura ativada com sucesso!', 'Fechar', { duration: 5000 });
        } else {
          this.snackBar.open('Assinatura pendente de confirmação', 'Fechar', { duration: 5000 });
        }
      },
      error: (error) => {
        console.error('Erro ao carregar assinatura:', error);
        this.snackBar.open('Erro ao verificar assinatura', 'Fechar', { duration: 5000 });
        this.isLoading = false;
      }
    });
  }
}
```

---

## 5️⃣ CHECKLIST DE VERIFICAÇÃO

### ✅ **Verifique se o Front-end Está Correto:**

- [ ] **Service de Assinaturas existe** e usa os endpoints corretos
- [ ] **Não chama APIs do Mercado Pago diretamente** (sempre via back-end)
- [ ] **Não usa Access Token do Mercado Pago** no front-end
- [ ] **Trata erros corretamente** (400, 401, 404, 500)
- [ ] **Redireciona para checkout** usando a URL retornada pelo back-end
- [ ] **Trata retorno do checkout** (success, cancel, pending)
- [ ] **Consulta assinatura atual** após retorno do checkout
- [ ] **Exibe status da assinatura** corretamente (ACTIVE, TRIAL, PENDING, etc.)

### ✅ **Verifique o Painel de Planos (se existir):**

- [ ] **Lista planos** usando `GET /api/plans`
- [ ] **Não cria planos no Mercado Pago** diretamente
- [x] **Cria planos no banco de dados** via `POST /api/plans` ✅ **AGORA EXISTE**
- [ ] **Não precisa sincronizar** antes de criar assinaturas
- [ ] **Planos são criados dinamicamente** quando usuário assina

---

## 6️⃣ EXEMPLOS DE CÓDIGO

### 📝 **Exemplo Completo: Service de Assinaturas**

```typescript
// subscription.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface SubscriptionDto {
  id: number;
  userId: number;
  status: 'TRIAL' | 'ACTIVE' | 'CANCELED' | 'EXPIRED' | 'PENDING';
  currentPeriodStart: string;
  currentPeriodEnd: string;
  planName: string;
  planPrice: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubscriptionResponse {
  checkoutUrl: string;
  sessionId: string;
}

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private apiUrl = `${environment.apiUrl}/api/subscription`;

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('Token não encontrado. Faça login.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  /**
   * Cria assinatura recorrente
   * Back-end agora usa PreapprovalPlan (correto)
   */
  createSubscription(planId: number): Observable<CreateSubscriptionResponse> {
    const headers = this.getAuthHeaders();
    return this.http.post<CreateSubscriptionResponse>(
      `${this.apiUrl}/create?planId=${planId}`,
      {},
      { headers }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Consulta assinatura atual
   */
  getCurrentSubscription(): Observable<SubscriptionDto> {
    const headers = this.getAuthHeaders();
    return this.http.get<SubscriptionDto>(`${this.apiUrl}/current`, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Cancela assinatura
   */
  cancelSubscription(): Observable<{ message: string }> {
    const headers = this.getAuthHeaders();
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/cancel`,
      {},
      { headers }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Portal do cliente
   */
  getCustomerPortal(): Observable<{ url: string }> {
    const headers = this.getAuthHeaders();
    return this.http.get<{ url: string }>(`${this.apiUrl}/customer-portal`, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Histórico de assinaturas
   */
  getHistory(): Observable<SubscriptionDto[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<SubscriptionDto[]>(`${this.apiUrl}/history`, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Tratamento de erros
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Erro desconhecido';
    
    if (error.error instanceof ErrorEvent) {
      // Erro do cliente
      errorMessage = `Erro: ${error.error.message}`;
    } else {
      // Erro do servidor
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Requisição inválida';
          break;
        case 401:
          errorMessage = 'Não autorizado. Faça login novamente.';
          break;
        case 404:
          errorMessage = 'Recurso não encontrado';
          break;
        case 500:
          errorMessage = 'Erro no servidor. Tente novamente mais tarde.';
          break;
        default:
          errorMessage = `Erro ${error.status}: ${error.message}`;
      }
    }
    
    return throwError(() => new Error(errorMessage));
  }
}
```

### 📝 **Exemplo: Componente de Seleção de Plano**

```typescript
// plan-selection.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlanService } from '../services/plan.service';
import { SubscriptionService } from '../services/subscription.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-plan-selection',
  templateUrl: './plan-selection.component.html',
  styleUrls: ['./plan-selection.component.scss']
})
export class PlanSelectionComponent implements OnInit {
  plans: any[] = [];
  isLoading = false;
  subscribingPlanId: number | null = null;

  constructor(
    private planService: PlanService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.isLoading = true;
    this.planService.getAllPlans().subscribe({
      next: (plans) => {
        this.plans = plans;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar planos:', error);
        this.snackBar.open('Erro ao carregar planos', 'Fechar', { duration: 5000 });
        this.isLoading = false;
      }
    });
  }

  subscribeToPlan(planId: number): void {
    if (this.isLoading || this.subscribingPlanId) return;

    this.subscribingPlanId = planId;
    this.isLoading = true;

    this.subscriptionService.createSubscription(planId).subscribe({
      next: (response) => {
        // Redireciona para checkout do Mercado Pago
        if (response.checkoutUrl) {
          window.location.href = response.checkoutUrl;
        } else {
          this.snackBar.open('Erro: URL de checkout não disponível', 'Fechar', { duration: 5000 });
          this.isLoading = false;
          this.subscribingPlanId = null;
        }
      },
      error: (error) => {
        console.error('Erro ao criar assinatura:', error);
        
        let message = 'Erro ao criar assinatura';
        if (error.message.includes('já possui')) {
          message = 'Você já possui uma assinatura ativa';
          setTimeout(() => this.router.navigate(['/subscription']), 2000);
        } else if (error.message.includes('autorizado')) {
          message = 'Sessão expirada. Redirecionando para login...';
          setTimeout(() => this.router.navigate(['/login']), 2000);
        }

        this.snackBar.open(message, 'Fechar', { duration: 5000 });
        this.isLoading = false;
        this.subscribingPlanId = null;
      }
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(price);
  }
}
```

---

## 📌 RESUMO FINAL

### ✅ **O QUE VOCÊ PRECISA SABER:**

1. **Endpoints não mudaram** - continue usando os mesmos
2. **Back-end foi corrigido** - agora usa PreapprovalPlan (correto)
3. **Front-end não precisa mudar** - apenas verificar se está usando os endpoints corretos
4. **Painel de planos está OK** - se apenas lista/cria planos no banco de dados
5. **Não precisa sincronizar** - planos são criados dinamicamente

### ⚠️ **O QUE NÃO FAZER:**

- ❌ Não chamar APIs do Mercado Pago diretamente
- ❌ Não usar Access Token no front-end
- ❌ Não criar PreapprovalPlan manualmente
- ❌ Não criar planos no Mercado Pago do front-end

### ✅ **O QUE FAZER:**

- ✅ Usar endpoints do back-end (`/api/subscription/*`)
- ✅ Tratar erros corretamente
- ✅ Redirecionar para checkout usando URL do back-end
- ✅ Consultar assinatura após retorno do checkout

---

## 📞 SUPORTE

Se tiver dúvidas ou problemas:

1. Verifique os logs do back-end
2. Verifique a resposta da API no Network do navegador
3. Consulte a documentação do Mercado Pago sobre PreapprovalPlan
4. Verifique se o Access Token está configurado no back-end

---

**Documento criado em:** 2025-01-15  
**Versão:** 2.0 - Assinaturas Recorrentes Corrigidas

