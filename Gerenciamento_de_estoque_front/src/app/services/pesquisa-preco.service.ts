import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { PesquisaPrecoResposta } from './admin.service';

@Injectable({ providedIn: 'root' })
export class PesquisaPrecoService {
  private url = `${environment.apiUrl}/api/pesquisa-preco`;

  constructor(private http: HttpClient) {}

  enviar(valorMin: number, valorMax: number, comentario?: string): Observable<PesquisaPrecoResposta> {
    return this.http.post<PesquisaPrecoResposta>(this.url, { valorMin, valorMax, comentario });
  }

  minhaResposta(): Observable<PesquisaPrecoResposta | null> {
    return this.http.get<PesquisaPrecoResposta>(`${this.url}/minha`, { observe: 'response' }).pipe(
      map((res) => (res.status === 204 ? null : res.body)),
      catchError(() => of(null))
    );
  }
}
