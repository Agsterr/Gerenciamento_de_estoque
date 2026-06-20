import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../environments/environment';

export interface AuthPublicConfig {
  registrationEnabled: boolean;
  demoEnabled: boolean;
  demoUsername: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly visitedKey = 'hasVisitedBefore';
  private readonly deviceFingerprintKey = 'deviceFingerprint';

  constructor(private apiService: ApiService, private router: Router) {}

  getDeviceFingerprint(): string {
    let fp = localStorage.getItem(this.deviceFingerprintKey);
    if (!fp) {
      fp = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `dev-${Date.now()}-${Math.random().toString(36).slice(2)}`;
      localStorage.setItem(this.deviceFingerprintKey, fp);
    }
    return fp;
  }

  getPublicConfig(): Observable<AuthPublicConfig> {
    return this.apiService.get<AuthPublicConfig>('/auth/config');
  }

  login(data: LoginRequest): Observable<LoginResponse> {
    const payload: LoginRequest = {
      ...data,
      deviceFingerprint: data.deviceFingerprint ?? this.getDeviceFingerprint(),
    };
    return this.apiService.post<LoginResponse>('/auth/login', payload).pipe(
      tap((response: LoginResponse) => {
        if (response?.token) {
          this.storeSession(response);
        }
      }),
      catchError((err) => {
        console.error('Erro ao fazer login:', err);
        const msg = err?.error?.error || 'Erro ao tentar fazer login. Verifique suas credenciais.';
        window.alert(msg);
        this.router.navigate(['/login']);
        return of({ token: '' });
      })
    );
  }

  loginDemo(demoUsername: string, demoPassword = 'demo123'): Observable<LoginResponse> {
    return this.login({ username: demoUsername, senha: demoPassword });
  }

  private storeSession(response: LoginResponse): void {
    localStorage.setItem('jwtToken', response.token);
    const decodedToken: any = jwtDecode(response.token);
    const userInfo = {
      username: decodedToken.sub,
      userId: decodedToken.user_id,
      orgId: decodedToken.org_id,
      roles: decodedToken.roles || [],
      demo: !!response.demo,
      ephemeral: !!response.ephemeral,
    };
    localStorage.setItem('loggedUser', JSON.stringify(userInfo));
    this.router.navigate(['/home']);
  }

  register(data: any): Observable<any> {
    return this.apiService.post('/auth/register', data);
  }

  getLoggedUser(): any {
    const userData = localStorage.getItem('loggedUser');
    return userData ? JSON.parse(userData) : null;
  }

  isDemoSession(): boolean {
    const user = this.getLoggedUser();
    return !!user?.demo || !!user?.ephemeral;
  }

  logout(): void {
    const token = localStorage.getItem('jwtToken');
    if (token) {
      this.apiService.post<void>('/auth/logout', {}).subscribe({
        complete: () => this.clearLocalSession(),
        error: () => this.clearLocalSession(),
      });
    } else {
      this.clearLocalSession();
    }
  }

  private clearLocalSession(): void {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('loggedUser');
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return this.isTokenValid();
  }

  hasSavedCredentials(): boolean {
    return !!(localStorage.getItem('savedUsername') && localStorage.getItem('savedPassword'));
  }

  hasVisitedBefore(): boolean {
    return localStorage.getItem(this.visitedKey) === 'true';
  }

  markVisited(): void {
    localStorage.setItem(this.visitedKey, 'true');
  }

  isTokenValid(): boolean {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      return false;
    }
    try {
      const decoded: { exp?: number } = jwtDecode(token);
      if (decoded.exp && decoded.exp * 1000 < Date.now()) {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('loggedUser');
        return false;
      }
      return true;
    } catch {
      return false;
    }
  }

  resolveEntryRoute(): string {
    if (this.isTokenValid()) {
      return '/dashboard';
    }
    if (this.hasSavedCredentials()) {
      return '/login';
    }
    return '/login';
  }

  hasRole(roleName: string): boolean {
    const user = this.getLoggedUser();
    if (!user || !Array.isArray(user.roles)) return false;
    return user.roles.some((role: any) => {
      if (typeof role === 'string') return role === roleName;
      return role?.nome === roleName
        || role?.name === roleName
        || role?.authority === roleName
        || role?.role === roleName;
    });
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN') || this.isMasterAdmin();
  }

  isMasterAdmin(): boolean {
    return this.hasRole('ROLE_SUPER_ADMIN');
  }

  /** Isento de gate de assinatura (bypass no JWT ou sessão demo). */
  hasSubscriptionBypass(): boolean {
    if (this.isDemoSession()) {
      return true;
    }
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      return false;
    }
    try {
      const decoded: any = jwtDecode(token);
      return decoded.bypass_subscription === true || decoded.bypassSubscription === true;
    } catch {
      return false;
    }
  }
}
