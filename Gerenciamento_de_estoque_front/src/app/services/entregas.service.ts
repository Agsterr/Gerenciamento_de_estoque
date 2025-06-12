// src/app/services/entregas.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EntregaResponse } from '../models/src/app/models/entrega/entrega-response.model';
import { PageEntregaResponse } from '../models/src/app/models/entrega/PageEntregaResponse.model';
import { EntregaRequest } from '../models/src/app/models/entrega/entrega-request.model';
import { EntregaComAvisoResponse } from '../models/src/app/models/entrega/entrega-com-aviso-response.model';

@Injectable({
  providedIn: 'root'
})
export class EntregasService {
  private apiUrl = 'http://localhost:8080/entregas';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('Token não encontrado. Usuário não autenticado.');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  private handleError(error: any) {
    // Aqui você pode personalizar o tratamento de erros
    console.error('Erro na requisição:', error);
    return throwError(() => error);
  }

  listarEntregas(page: number, size: number): Observable<PageEntregaResponse> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PageEntregaResponse>(this.apiUrl, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  criarEntrega(entrega: EntregaRequest): Observable<EntregaComAvisoResponse> {
    return this.http.post<EntregaComAvisoResponse>(this.apiUrl, entrega, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  editarEntrega(id: number, entrega: EntregaRequest): Observable<EntregaResponse> {
    return this.http.put<EntregaResponse>(`${this.apiUrl}/${id}`, entrega, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  deletarEntrega(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  porDia(dia: string): Observable<EntregaResponse[]> {
    const params = new HttpParams().set('dia', dia);
    return this.http.get<EntregaResponse[]>(`${this.apiUrl}/por-dia`, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  porPeriodo(inicio: string, fim: string): Observable<EntregaResponse[]> {
    const params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    return this.http.get<EntregaResponse[]>(`${this.apiUrl}/por-periodo`, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  porMes(mes: number, ano: number): Observable<EntregaResponse[]> {
    const params = new HttpParams()
      .set('mes', mes)
      .set('ano', ano);
    return this.http.get<EntregaResponse[]>(`${this.apiUrl}/por-mes`, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  porAno(ano: number): Observable<EntregaResponse[]> {
    const params = new HttpParams().set('ano', ano);
    return this.http.get<EntregaResponse[]>(`${this.apiUrl}/por-ano`, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  porConsumidor(consumidorId: number): Observable<EntregaResponse[]> {
    return this.http.get<EntregaResponse[]>(`${this.apiUrl}/por-consumidor/${consumidorId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  porConsumidorPeriodo(consumidorId: number, inicio: string, fim: string): Observable<EntregaResponse[]> {
    const params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim);
    return this.http.get<EntregaResponse[]>(`${this.apiUrl}/por-consumidor/${consumidorId}/periodo`, {
      headers: this.getAuthHeaders(),
      params
    }).pipe(
      catchError(this.handleError)
    );
  }
}