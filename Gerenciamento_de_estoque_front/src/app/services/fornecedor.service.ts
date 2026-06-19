import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Fornecedor {
  id: number; nome: string; cnpj?: string; email?: string;
  telefone?: string; endereco?: string; ativo: boolean; orgId: number;
}

@Injectable({ providedIn: 'root' })
export class FornecedorService {
  private url = `${environment.apiUrl}/fornecedores`;
  constructor(private http: HttpClient) {}
  listar(page = 0, size = 20): Observable<{ content: Fornecedor[]; totalElements: number }> {
    return this.http.get<{ content: Fornecedor[]; totalElements: number }>(`${this.url}?page=${page}&size=${size}`);
  }
  criar(data: Partial<Fornecedor>): Observable<Fornecedor> { return this.http.post<Fornecedor>(this.url, data); }
  atualizar(id: number, data: Partial<Fornecedor>): Observable<Fornecedor> { return this.http.put<Fornecedor>(`${this.url}/${id}`, data); }
  excluir(id: number): Observable<{ message: string }> { return this.http.delete<{ message: string }>(`${this.url}/${id}`); }
}
