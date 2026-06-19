# 📘 ENDPOINT: CRIAR PLANO - DOCUMENTAÇÃO COMPLETA

> **Para desenvolvedores Angular**  
> **Data:** 2025-01-15

---

## ✅ ENDPOINT IMPLEMENTADO

O endpoint `POST /api/plans` **foi criado** e está funcionando!

---

## 📋 DETALHES DO ENDPOINT

### **Criar Plano**

**Método:** `POST`  
**URL:** `/api/plans`  
**Autenticação:** ✅ **OBRIGATÓRIA** (Role: ADMIN)  
**Content-Type:** `application/json`

---

## 📤 BODY DA REQUISIÇÃO (POST)

### ✅ **GET vs POST - Diferença:**

- **GET** (`GET /api/plans`) - **Só traz dados**, não precisa de body
- **POST** (`POST /api/plans`) - **Cria dados**, precisa de body com os dados do plano

### 📝 **Formato do Body (JSON):**

```json
{
  "name": "Nome do Plano",
  "description": "Descrição detalhada do plano",
  "price": 99.90,
  "type": "PROFESSIONAL",
  "maxUsers": 10,
  "maxProducts": 500,
  "maxOrganizations": null,  // ⚠️ Opcional - pode ser null (não faz sentido limitar no modelo atual)
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true
}
```

### 📌 **Campos Obrigatórios:**

| Campo | Tipo | Descrição | Exemplo |
|-------|------|-----------|---------|
| `name` | string | Nome do plano (único) | `"Plano Premium"` |
| `price` | number | Preço em reais (deve ser > 0) | `99.90` |
| `type` | string | Tipo do plano | `"BASIC"`, `"PROFESSIONAL"`, `"ENTERPRISE"` |

### 📌 **Campos Opcionais:**

| Campo | Tipo | Padrão | Descrição |
|-------|------|--------|-----------|
| `description` | string | `null` | Descrição do plano |
| `maxUsers` | number | `null` | Limite de usuários (null = ilimitado) |
| `maxProducts` | number | `null` | Limite de produtos (null = ilimitado) |
| `maxOrganizations` | number | `null` | ⚠️ **OPCIONAL** - Limite de organizações (null = ilimitado). **NOTA:** No modelo atual, cada usuário pertence a 1 organização, então este limite pode não fazer sentido. Recomenda-se sempre usar `null`. |
| `hasReports` | boolean | `false` | Acesso a relatórios |
| `hasAdvancedAnalytics` | boolean | `false` | Analytics avançado |
| `hasApiAccess` | boolean | `false` | Acesso à API |
| `isActive` | boolean | `true` | Plano ativo |

---

## 📥 RESPOSTA

### ✅ **Sucesso (201 Created):**

```json
{
  "id": 1,
  "name": "Nome do Plano",
  "description": "Descrição detalhada do plano",
  "price": 99.90,
  "type": "PROFESSIONAL",
  "maxUsers": 10,
  "maxProducts": 500,
  "maxOrganizations": null,  // ⚠️ Opcional - pode ser null (não faz sentido limitar no modelo atual)
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true,
  "stripePriceId": null,
  "stripeProductId": null,
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

### ❌ **Erros Possíveis:**

#### **400 Bad Request**
```json
{
  "error": "Nome do plano é obrigatório"
}
```
ou
```json
{
  "error": "Preço do plano deve ser maior que zero"
}
```

#### **401 Unauthorized**
```json
{
  "error": "Token inválido ou expirado"
}
```
ou
```json
{
  "error": "Acesso negado. Requer role ADMIN"
}
```

#### **409 Conflict**
```json
{
  "error": "Já existe um plano com este nome"
}
```

---

## 💻 EXEMPLO ANGULAR

### **1. Service (PlanService):**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Plan {
  id?: number;
  name: string;
  description?: string;
  price: number;
  type: 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';
  maxUsers?: number;
  maxProducts?: number;
  maxOrganizations?: number;
  hasReports?: boolean;
  hasAdvancedAnalytics?: boolean;
  hasApiAccess?: boolean;
  isActive?: boolean;
}

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
   * Lista todos os planos (GET - não precisa de body)
   */
  getAllPlans(): Observable<Plan[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<Plan[]>(this.apiUrl, { headers });
  }

  /**
   * Cria um novo plano (POST - precisa de body)
   */
  createPlan(plan: Plan): Observable<Plan> {
    const headers = this.getAuthHeaders();
    return this.http.post<Plan>(this.apiUrl, plan, { headers });
  }

  /**
   * Atualiza um plano existente (PUT - precisa de body)
   */
  updatePlan(id: number, plan: Partial<Plan>): Observable<Plan> {
    const headers = this.getAuthHeaders();
    return this.http.put<Plan>(`${this.apiUrl}/${id}`, plan, { headers });
  }

  /**
   * Deleta um plano (DELETE - não precisa de body)
   */
  deletePlan(id: number): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers });
  }
}
```

### **2. Componente (PlanFormComponent):**

```typescript
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PlanService, Plan } from '../services/plan.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

@Component({
  selector: 'app-plan-form',
  templateUrl: './plan-form.component.html'
})
export class PlanFormComponent {
  planForm: FormGroup;
  isLoading = false;

  planTypes = ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'];

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
      type: ['BASIC', Validators.required],
      maxUsers: [null],
      maxProducts: [null],
      maxOrganizations: [null],
      hasReports: [false],
      hasAdvancedAnalytics: [false],
      hasApiAccess: [false],
      isActive: [true]
    });
  }

  /**
   * Cria plano - POST com body
   */
  onSubmit(): void {
    if (this.planForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    const planData: Plan = this.planForm.value;

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

### **3. Template HTML:**

```html
<form [formGroup]="planForm" (ngSubmit)="onSubmit()">
  <mat-form-field>
    <mat-label>Nome do Plano *</mat-label>
    <input matInput formControlName="name" required>
    <mat-error *ngIf="planForm.get('name')?.hasError('required')">
      Nome é obrigatório
    </mat-error>
  </mat-form-field>

  <mat-form-field>
    <mat-label>Descrição</mat-label>
    <textarea matInput formControlName="description" rows="3"></textarea>
  </mat-form-field>

  <mat-form-field>
    <mat-label>Preço (R$) *</mat-label>
    <input matInput type="number" formControlName="price" step="0.01" min="0.01" required>
    <mat-error *ngIf="planForm.get('price')?.hasError('required')">
      Preço é obrigatório
    </mat-error>
  </mat-form-field>

  <mat-form-field>
    <mat-label>Tipo *</mat-label>
    <mat-select formControlName="type" required>
      <mat-option value="BASIC">Básico</mat-option>
      <mat-option value="PROFESSIONAL">Profissional</mat-option>
      <mat-option value="ENTERPRISE">Empresarial</mat-option>
    </mat-select>
  </mat-form-field>

  <mat-form-field>
    <mat-label>Limite de Usuários</mat-label>
    <input matInput type="number" formControlName="maxUsers" min="1">
    <mat-hint>Deixe vazio para ilimitado</mat-hint>
  </mat-form-field>

  <mat-form-field>
    <mat-label>Limite de Produtos</mat-label>
    <input matInput type="number" formControlName="maxProducts" min="1">
    <mat-hint>Deixe vazio para ilimitado</mat-hint>
  </mat-form-field>

  <mat-checkbox formControlName="hasReports">Acesso a Relatórios</mat-checkbox>
  <mat-checkbox formControlName="hasAdvancedAnalytics">Analytics Avançado</mat-checkbox>
  <mat-checkbox formControlName="hasApiAccess">Acesso à API</mat-checkbox>
  <mat-checkbox formControlName="isActive">Plano Ativo</mat-checkbox>

  <button mat-raised-button color="primary" type="submit" [disabled]="planForm.invalid || isLoading">
    <span *ngIf="!isLoading">Criar Plano</span>
    <span *ngIf="isLoading">Criando...</span>
  </button>
</form>
```

---

## 📌 RESUMO

### ✅ **GET vs POST:**

| Método | Endpoint | Body? | Descrição |
|--------|----------|-------|-----------|
| **GET** | `/api/plans` | ❌ Não | Lista planos (só traz dados) |
| **GET** | `/api/plans/{id}` | ❌ Não | Busca plano por ID (só traz dados) |
| **POST** | `/api/plans` | ✅ **SIM** | Cria novo plano (precisa de body) |
| **PUT** | `/api/plans/{id}` | ✅ **SIM** | Atualiza plano (precisa de body) |
| **DELETE** | `/api/plans/{id}` | ❌ Não | Deleta plano (não precisa de body) |

### ✅ **Body Obrigatório em POST:**

**SIM!** Quando você faz `POST /api/plans`, precisa enviar o body com os dados do plano:

```typescript
// ✅ CORRETO - POST com body
this.http.post('/api/plans', {
  name: 'Plano Premium',
  price: 99.90,
  type: 'PROFESSIONAL'
}, { headers });

// ❌ ERRADO - POST sem body
this.http.post('/api/plans', {}, { headers });
```

---

## ✅ CHECKLIST

- [x] Endpoint `POST /api/plans` implementado
- [x] Endpoint `PUT /api/plans/{id}` implementado
- [x] Endpoint `DELETE /api/plans/{id}` implementado
- [x] Validação de campos obrigatórios
- [x] Verificação de nome duplicado
- [x] Requer role ADMIN
- [x] Documentação completa

---

**Documento criado em:** 2025-01-15  
**Última atualização:** 2025-01-15

