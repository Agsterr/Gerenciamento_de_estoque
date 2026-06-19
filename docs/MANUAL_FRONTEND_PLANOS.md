# 📘 MANUAL FRONTEND: GERENCIAMENTO DE PLANOS

> **Documentação completa para integração front-end**  
> **Data:** 2025-01-15  
> **Última atualização:** 2025-01-15

---

## 📋 ÍNDICE

1. [Visão Geral](#visão-geral)
2. [Endpoints Disponíveis](#endpoints-disponíveis)
3. [Planos Pré-definidos](#planos-pré-definidos)
4. [Implementação: Pré-preenchimento de Formulário](#implementação-pré-preenchimento-de-formulário)
5. [Modelos de Dados](#modelos-de-dados)
6. [Exemplos de Código](#exemplos-de-código)

---

## 🎯 VISÃO GERAL

O sistema possui **3 planos pré-definidos** no banco de dados:
- **Básico** (BASIC) - R$ 29,90/mês
- **Profissional** (PROFESSIONAL) - R$ 79,90/mês
- **Empresarial** (ENTERPRISE) - R$ 199,90/mês

Quando o usuário seleciona um tipo de plano no formulário, os campos devem ser **automaticamente pré-preenchidos** com os valores do plano correspondente.

---

## 🔌 ENDPOINTS DISPONÍVEIS

### Base URL
```
/api/plans
```

### 1. Listar Todos os Planos Ativos

**Método:** `GET`  
**Endpoint:** `/api/plans`  
**Autenticação:** ✅ Requerida  
**Body:** ❌ Não precisa

**Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Básico",
    "description": "Plano ideal para pequenas empresas que estão começando...",
    "price": 29.90,
    "type": "BASIC",
    "maxUsers": 5,
    "maxProducts": 1000,
    "maxOrganizations": null,
    "hasReports": true,
    "hasAdvancedAnalytics": false,
    "hasApiAccess": false,
    "isActive": true,
    "stripePriceId": null,
    "stripeProductId": null,
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00"
  },
  {
    "id": 2,
    "name": "Profissional",
    "description": "Plano completo para empresas em crescimento...",
    "price": 79.90,
    "type": "PROFESSIONAL",
    "maxUsers": 25,
    "maxProducts": 10000,
    "maxOrganizations": null,
    "hasReports": true,
    "hasAdvancedAnalytics": true,
    "hasApiAccess": false,
    "isActive": true,
    "stripePriceId": null,
    "stripeProductId": null,
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00"
  },
  {
    "id": 3,
    "name": "Empresarial",
    "description": "Solução enterprise com recursos ilimitados...",
    "price": 199.90,
    "type": "ENTERPRISE",
    "maxUsers": null,
    "maxProducts": null,
    "maxOrganizations": null,
    "hasReports": true,
    "hasAdvancedAnalytics": true,
    "hasApiAccess": true,
    "isActive": true,
    "stripePriceId": null,
    "stripeProductId": null,
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00"
  }
]
```

---

### 2. Buscar Plano por ID

**Método:** `GET`  
**Endpoint:** `/api/plans/{id}`  
**Autenticação:** ✅ Requerida  
**Parâmetros Path:**
- `id` (number): ID do plano

**Resposta (200 OK):** Objeto Plan completo  
**Resposta (404 Not Found):** Plano não encontrado

---

### 3. Buscar Plano por Tipo

**Método:** `GET`  
**Endpoint:** `/api/plans/type/{type}`  
**Autenticação:** ✅ Requerida  
**Parâmetros Path:**
- `type` (string): `BASIC`, `PROFESSIONAL` ou `ENTERPRISE`

**Resposta (200 OK):** Objeto Plan completo  
**Resposta (404 Not Found):** Plano não encontrado

**Exemplo:**
```
GET /api/plans/type/BASIC
```

---

### 4. Obter Recursos do Plano

**Método:** `GET`  
**Endpoint:** `/api/plans/{id}/features`  
**Autenticação:** ✅ Requerida  
**Parâmetros Path:**
- `id` (number): ID do plano

**Resposta (200 OK):**
```json
{
  "name": "Básico",
  "description": "Plano ideal para pequenas empresas...",
  "price": 29.90,
  "type": "BASIC",
  "maxUsers": 5,
  "maxProducts": 1000,
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "mercadoPagoReady": true
}
```

**Nota:** Este endpoint **NÃO retorna** `maxOrganizations` porque organizações não são limitadas.

---

### 5. Criar Novo Plano

**Método:** `POST`  
**Endpoint:** `/api/plans`  
**Autenticação:** ✅ Requerida (Role: ADMIN)  
**Content-Type:** `application/json`  
**Body:** ✅ Obrigatório

**Body da Requisição:**
```json
{
  "name": "Nome do Plano",
  "description": "Descrição do plano",
  "price": 99.90,
  "type": "PROFESSIONAL",
  "maxUsers": 10,
  "maxProducts": 500,
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true
}
```

**Campos Obrigatórios:**
- `name` (string): Nome do plano (único)
- `price` (number): Preço em reais (deve ser > 0)
- `type` (string): `BASIC`, `PROFESSIONAL` ou `ENTERPRISE`

**Campos Opcionais:**
- `description` (string): Descrição do plano
- `maxUsers` (number | null): Limite de usuários (null = ilimitado)
- `maxProducts` (number | null): Limite de produtos (null = ilimitado)
- `hasReports` (boolean): Acesso a relatórios (padrão: false)
- `hasAdvancedAnalytics` (boolean): Analytics avançado (padrão: false)
- `hasApiAccess` (boolean): Acesso à API (padrão: false)
- `isActive` (boolean): Plano ativo (padrão: true)

**⚠️ IMPORTANTE:** 
- **NÃO enviar** `maxOrganizations` - organizações não são limitadas
- **NÃO enviar** `id`, `stripePriceId`, `stripeProductId`, `createdAt`, `updatedAt` - são gerados automaticamente

**Resposta (201 Created):** Objeto Plan criado

**Resposta (400 Bad Request):**
```json
{
  "error": "Nome do plano é obrigatório"
}
```

**Resposta (409 Conflict):**
```json
{
  "error": "Já existe um plano com este nome"
}
```

---

### 6. Atualizar Plano Existente

**Método:** `PUT`  
**Endpoint:** `/api/plans/{id}`  
**Autenticação:** ✅ Requerida (Role: ADMIN)  
**Content-Type:** `application/json`  
**Parâmetros Path:**
- `id` (number): ID do plano
**Body:** ✅ Obrigatório

**Body da Requisição:** Mesmo formato do POST (campos opcionais)

**Resposta (200 OK):** Objeto Plan atualizado

---

### 7. Deletar/Desativar Plano

**Método:** `DELETE`  
**Endpoint:** `/api/plans/{id}`  
**Autenticação:** ✅ Requerida (Role: ADMIN)  
**Parâmetros Path:**
- `id` (number): ID do plano
**Body:** ❌ Não precisa

**Resposta (204 No Content):** Plano deletado/desativado

---

## 📊 PLANOS PRÉ-DEFINIDOS

Os seguintes planos já estão cadastrados no banco de dados:

### 🟢 Plano Básico (BASIC)

```json
{
  "name": "Básico",
  "description": "Plano ideal para pequenas empresas que estão começando. Inclui funcionalidades essenciais para gerenciamento de estoque com relatórios básicos e suporte por email.",
  "price": 29.90,
  "type": "BASIC",
  "maxUsers": 5,
  "maxProducts": 1000,
  "maxOrganizations": null,
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true
}
```

### 🟡 Plano Profissional (PROFESSIONAL)

```json
{
  "name": "Profissional",
  "description": "Plano completo para empresas em crescimento. Inclui analytics avançado, relatórios detalhados e suporte prioritário para otimizar suas operações.",
  "price": 79.90,
  "type": "PROFESSIONAL",
  "maxUsers": 25,
  "maxProducts": 10000,
  "maxOrganizations": null,
  "hasReports": true,
  "hasAdvancedAnalytics": true,
  "hasApiAccess": false,
  "isActive": true
}
```

### 🔴 Plano Empresarial (ENTERPRISE)

```json
{
  "name": "Empresarial",
  "description": "Solução enterprise com recursos ilimitados. Inclui acesso completo à API, analytics avançado, relatórios personalizados e suporte dedicado 24/7.",
  "price": 199.90,
  "type": "ENTERPRISE",
  "maxUsers": null,
  "maxProducts": null,
  "maxOrganizations": null,
  "hasReports": true,
  "hasAdvancedAnalytics": true,
  "hasApiAccess": true,
  "isActive": true
}
```

**Nota:** `null` em `maxUsers` ou `maxProducts` significa **ilimitado**.

---

## 🎨 IMPLEMENTAÇÃO: PRÉ-PREENCHIMENTO DE FORMULÁRIO

### Fluxo Recomendado

1. **Ao carregar o formulário:**
   - Buscar todos os planos com `GET /api/plans`
   - Armazenar os planos em memória

2. **Ao selecionar o tipo de plano:**
   - Buscar o plano correspondente ao tipo selecionado
   - Pré-preencher todos os campos do formulário com os valores do plano

3. **Ao criar o plano no Mercado Pago:**
   - O formulário já estará preenchido
   - O usuário pode ajustar se necessário
   - Enviar para `POST /api/plans` quando criar no Mercado Pago

### Exemplo de Implementação

```typescript
// 1. Buscar planos ao carregar componente
ngOnInit() {
  this.loadPlans();
}

loadPlans() {
  this.planService.getAllPlans().subscribe({
    next: (plans) => {
      this.availablePlans = plans;
      // Armazenar planos por tipo para acesso rápido
      this.plansByType = {};
      plans.forEach(plan => {
        this.plansByType[plan.type] = plan;
      });
    },
    error: (error) => {
      console.error('Erro ao carregar planos:', error);
    }
  });
}

// 2. Pré-preencher formulário ao selecionar tipo
onPlanTypeChange(planType: string) {
  const plan = this.plansByType[planType];
  
  if (plan) {
    // Pré-preencher todos os campos
    this.planForm.patchValue({
      name: plan.name,
      description: plan.description,
      price: plan.price,
      type: plan.type,
      maxUsers: plan.maxUsers,
      maxProducts: plan.maxProducts,
      hasReports: plan.hasReports,
      hasAdvancedAnalytics: plan.hasAdvancedAnalytics,
      hasApiAccess: plan.hasApiAccess,
      isActive: plan.isActive
    });
    
    // Desabilitar campos que não devem ser editados
    this.planForm.get('type')?.disable();
  }
}
```

---

## 📦 MODELOS DE DADOS

### Interface TypeScript - Plan

```typescript
export interface Plan {
  id?: number;
  name: string;
  description?: string;
  price: number;
  type: 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';
  maxUsers?: number | null;
  maxProducts?: number | null;
  hasReports?: boolean;
  hasAdvancedAnalytics?: boolean;
  hasApiAccess?: boolean;
  isActive?: boolean;
  stripePriceId?: string | null;
  stripeProductId?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
```

**⚠️ IMPORTANTE:**
- **NÃO incluir** `maxOrganizations` na interface - organizações não são limitadas
- `maxUsers` e `maxProducts` podem ser `null` (ilimitado)
- Campos com `?` são opcionais

---

## 💻 EXEMPLOS DE CÓDIGO

### 1. Service (PlanService)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Plan } from '../models/plan.model';

@Injectable({
  providedIn: 'root'
})
export class PlanService {
  private apiUrl = `${environment.apiUrl}/api/plans`;

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('Token não encontrado. Faça login.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  /**
   * Lista todos os planos ativos
   */
  getAllPlans(): Observable<Plan[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<Plan[]>(this.apiUrl, { headers });
  }

  /**
   * Busca plano por ID
   */
  getPlanById(id: number): Observable<Plan> {
    const headers = this.getAuthHeaders();
    return this.http.get<Plan>(`${this.apiUrl}/${id}`, { headers });
  }

  /**
   * Busca plano por tipo (BASIC, PROFESSIONAL, ENTERPRISE)
   */
  getPlanByType(type: string): Observable<Plan> {
    const headers = this.getAuthHeaders();
    return this.http.get<Plan>(`${this.apiUrl}/type/${type}`, { headers });
  }

  /**
   * Cria um novo plano
   */
  createPlan(plan: Plan): Observable<Plan> {
    const headers = this.getAuthHeaders();
    return this.http.post<Plan>(this.apiUrl, plan, { headers });
  }

  /**
   * Atualiza um plano existente
   */
  updatePlan(id: number, plan: Partial<Plan>): Observable<Plan> {
    const headers = this.getAuthHeaders();
    return this.http.put<Plan>(`${this.apiUrl}/${id}`, plan, { headers });
  }

  /**
   * Deleta um plano
   */
  deletePlan(id: number): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers });
  }
}
```

---

### 2. Componente com Pré-preenchimento

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PlanService, Plan } from '../services/plan.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

@Component({
  selector: 'app-plan-form',
  templateUrl: './plan-form.component.html',
  styleUrls: ['./plan-form.component.scss']
})
export class PlanFormComponent implements OnInit {
  planForm: FormGroup;
  isLoading = false;
  availablePlans: Plan[] = [];
  plansByType: { [key: string]: Plan } = {};

  planTypes = [
    { value: 'BASIC', label: 'Básico' },
    { value: 'PROFESSIONAL', label: 'Profissional' },
    { value: 'ENTERPRISE', label: 'Empresarial' }
  ];

  constructor(
    private fb: FormBuilder,
    private planService: PlanService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.planForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: [''],
      price: [0, [Validators.required, Validators.min(0.01)]],
      type: ['', Validators.required],
      maxUsers: [null],
      maxProducts: [null],
      hasReports: [false],
      hasAdvancedAnalytics: [false],
      hasApiAccess: [false],
      isActive: [true]
    });
  }

  ngOnInit(): void {
    this.loadPlans();
  }

  /**
   * Carrega todos os planos do banco de dados
   */
  loadPlans(): void {
    this.planService.getAllPlans().subscribe({
      next: (plans) => {
        this.availablePlans = plans;
        // Criar mapa por tipo para acesso rápido
        plans.forEach(plan => {
          this.plansByType[plan.type] = plan;
        });
      },
      error: (error) => {
        console.error('Erro ao carregar planos:', error);
        this.snackBar.open('Erro ao carregar planos', 'Fechar', { duration: 3000 });
      }
    });
  }

  /**
   * Pré-preenche formulário quando tipo de plano é selecionado
   */
  onPlanTypeChange(): void {
    const selectedType = this.planForm.get('type')?.value;
    
    if (!selectedType) {
      return;
    }

    const plan = this.plansByType[selectedType];
    
    if (plan) {
      // Pré-preencher todos os campos com valores do plano
      this.planForm.patchValue({
        name: plan.name,
        description: plan.description,
        price: plan.price,
        maxUsers: plan.maxUsers,
        maxProducts: plan.maxProducts,
        hasReports: plan.hasReports ?? false,
        hasAdvancedAnalytics: plan.hasAdvancedAnalytics ?? false,
        hasApiAccess: plan.hasApiAccess ?? false,
        isActive: plan.isActive ?? true
      });

      // Desabilitar campo tipo (não deve ser alterado após seleção)
      this.planForm.get('type')?.disable();
      
      this.snackBar.open(`Formulário pré-preenchido com dados do plano ${plan.name}`, 'Fechar', { 
        duration: 3000 
      });
    } else {
      this.snackBar.open('Plano não encontrado', 'Fechar', { duration: 3000 });
    }
  }

  /**
   * Cria plano no Mercado Pago
   */
  onSubmit(): void {
    if (this.planForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    
    // Reabilitar campo tipo para envio
    this.planForm.get('type')?.enable();
    
    const planData: Plan = this.planForm.value;

    // Remover campos que não devem ser enviados
    delete planData.id;
    delete planData.stripePriceId;
    delete planData.stripeProductId;
    delete planData.createdAt;
    delete planData.updatedAt;

    this.planService.createPlan(planData).subscribe({
      next: (createdPlan) => {
        this.snackBar.open('Plano criado com sucesso!', 'Fechar', { duration: 5000 });
        this.router.navigate(['/plans']);
      },
      error: (error) => {
        console.error('Erro ao criar plano:', error);
        
        let errorMessage = 'Erro ao criar plano';
        
        if (error.status === 400) {
          errorMessage = error.error?.error || 'Dados inválidos';
        } else if (error.status === 401) {
          errorMessage = 'Não autorizado. Faça login como ADMIN.';
        } else if (error.status === 409) {
          errorMessage = 'Já existe um plano com este nome';
        } else if (error.status === 500) {
          errorMessage = 'Erro no servidor. Tente novamente.';
        }

        this.snackBar.open(errorMessage, 'Fechar', { duration: 5000 });
        this.isLoading = false;
      }
    });
  }
}
```

---

### 3. Template HTML

```html
<form [formGroup]="planForm" (ngSubmit)="onSubmit()">
  
  <!-- Tipo de Plano (seletor) -->
  <mat-form-field appearance="outline">
    <mat-label>Tipo de Plano *</mat-label>
    <mat-select formControlName="type" (selectionChange)="onPlanTypeChange()" required>
      <mat-option *ngFor="let planType of planTypes" [value]="planType.value">
        {{ planType.label }}
      </mat-option>
    </mat-select>
    <mat-hint>Selecione o tipo para pré-preencher o formulário</mat-hint>
  </mat-form-field>

  <!-- Nome do Plano -->
  <mat-form-field appearance="outline">
    <mat-label>Nome do Plano *</mat-label>
    <input matInput formControlName="name" required>
    <mat-error *ngIf="planForm.get('name')?.hasError('required')">
      Nome é obrigatório
    </mat-error>
  </mat-form-field>

  <!-- Descrição -->
  <mat-form-field appearance="outline">
    <mat-label>Descrição</mat-label>
    <textarea matInput formControlName="description" rows="3"></textarea>
  </mat-form-field>

  <!-- Preço -->
  <mat-form-field appearance="outline">
    <mat-label>Preço (R$) *</mat-label>
    <input matInput type="number" formControlName="price" step="0.01" min="0.01" required>
    <span matPrefix>R$&nbsp;</span>
    <mat-error *ngIf="planForm.get('price')?.hasError('required')">
      Preço é obrigatório
    </mat-error>
  </mat-form-field>

  <!-- Limite de Usuários -->
  <mat-form-field appearance="outline">
    <mat-label>Limite de Usuários</mat-label>
    <input matInput type="number" formControlName="maxUsers" min="1">
    <mat-hint>Deixe vazio para ilimitado</mat-hint>
  </mat-form-field>

  <!-- Limite de Produtos -->
  <mat-form-field appearance="outline">
    <mat-label>Limite de Produtos</mat-label>
    <input matInput type="number" formControlName="maxProducts" min="1">
    <mat-hint>Deixe vazio para ilimitado</mat-hint>
  </mat-form-field>

  <!-- Recursos -->
  <div class="features-section">
    <h3>Recursos do Plano</h3>
    
    <mat-checkbox formControlName="hasReports">
      Acesso a Relatórios
    </mat-checkbox>
    
    <mat-checkbox formControlName="hasAdvancedAnalytics">
      Analytics Avançado
    </mat-checkbox>
    
    <mat-checkbox formControlName="hasApiAccess">
      Acesso à API
    </mat-checkbox>
    
    <mat-checkbox formControlName="isActive">
      Plano Ativo
    </mat-checkbox>
  </div>

  <!-- Botões -->
  <div class="actions">
    <button mat-raised-button type="button" (click)="router.navigate(['/plans'])">
      Cancelar
    </button>
    <button mat-raised-button color="primary" type="submit" [disabled]="planForm.invalid || isLoading">
      <span *ngIf="!isLoading">Criar Plano no Mercado Pago</span>
      <span *ngIf="isLoading">Criando...</span>
    </button>
  </div>

</form>
```

---

## ✅ CHECKLIST DE IMPLEMENTAÇÃO

- [ ] Criar interface `Plan` sem `maxOrganizations`
- [ ] Criar service `PlanService` com todos os métodos
- [ ] Implementar componente de formulário
- [ ] Buscar planos ao carregar componente (`GET /api/plans`)
- [ ] Implementar pré-preenchimento ao selecionar tipo
- [ ] Remover campo "Max Organizações" do formulário
- [ ] Implementar criação de plano (`POST /api/plans`)
- [ ] Tratar erros (400, 401, 409, 500)
- [ ] Exibir mensagens de sucesso/erro

---

## 📌 RESUMO RÁPIDO

### Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/plans` | Lista todos os planos |
| `GET` | `/api/plans/type/{type}` | Busca plano por tipo |
| `POST` | `/api/plans` | Cria novo plano |

### Valores Pré-definidos

| Tipo | Preço | Usuários | Produtos |
|------|-------|----------|----------|
| BASIC | R$ 29,90 | 5 | 1.000 |
| PROFESSIONAL | R$ 79,90 | 25 | 10.000 |
| ENTERPRISE | R$ 199,90 | Ilimitado | Ilimitado |

### ⚠️ IMPORTANTE

- **NÃO incluir** `maxOrganizations` - organizações não são limitadas
- **Sempre buscar** planos do banco antes de pré-preencher
- **Desabilitar** campo `type` após seleção
- **Reabilitar** campo `type` antes de enviar POST

---

**Documento criado em:** 2025-01-15  
**Última atualização:** 2025-01-15







