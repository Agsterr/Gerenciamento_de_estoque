import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Usuario } from '../models/usuario.model';

@Injectable({
  providedIn: 'root',
})
export class UsuarioService {
  private apiUrl = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('Token não encontrado. Usuário não autenticado.');
    }
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    });
  }

  listarAtivos(page = 0, size = 20): Observable<{ content: Usuario[]; totalElements: number; totalPages: number }> {
    return this.http.get<{ content: Usuario[]; totalElements: number; totalPages: number }>(
      `${this.apiUrl}/ativos?page=${page}&size=${size}`,
      { headers: this.getAuthHeaders() }
    );
  }

  ativar(id: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(
      `${this.apiUrl}/${id}/ativar`,
      {},
      { headers: this.getAuthHeaders() }
    );
  }

  desativar(id: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(
      `${this.apiUrl}/${id}/desativar`,
      {},
      { headers: this.getAuthHeaders() }
    );
  }

  reativar(username: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/reativar-usuario`,
      { username },
      { headers: this.getAuthHeaders() }
    );
  }

  resetSenha(id: number): Observable<{ username: string; temporaryPassword: string }> {
    return this.http.post<{ username: string; temporaryPassword: string }>(
      `${this.apiUrl}/${id}/reset-password`,
      {},
      { headers: this.getAuthHeaders() }
    );
  }
}
