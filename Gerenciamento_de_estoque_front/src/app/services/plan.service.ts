import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Plan } from '../models/plan.model';

@Injectable({
  providedIn: 'root',
})
export class PlanService {
  private apiUrl = `${environment.apiUrl}/api/plans`;

  constructor(private http: HttpClient) {}

  listarAtivos(): Observable<Plan[]> {
    return this.http.get<Plan[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Plan> {
    return this.http.get<Plan>(`${this.apiUrl}/${id}`);
  }
}
