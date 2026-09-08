import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from './auth.interceptor';
import { AppConfigService } from '../config/app-config.service';

describe('authInterceptor CSRF', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        AuthService,
        { provide: AppConfigService, useValue: { apiBaseUrl: 'http://api.test/api' } }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('sends X-XSRF-TOKEN from AuthService memory on auth POST', async () => {
    const csrfPromise = auth.fetchCsrf();
    httpMock.expectOne('http://api.test/api/auth/csrf').flush({ token: 'memory-csrf-token' });
    await csrfPromise;

    http.post('http://api.test/api/auth/login', { email: 'a@example.com', password: 'password123' }).subscribe();
    const login = httpMock.expectOne('http://api.test/api/auth/login');
    expect(login.request.headers.get('X-XSRF-TOKEN')).toBe('memory-csrf-token');
    login.flush({ accessToken: 'jwt', user: {} });
  });

  it('does not read CSRF from document.cookie', async () => {
    document.cookie = 'XSRF-TOKEN=wrong-cookie-value';
    const csrfPromise = auth.fetchCsrf();
    httpMock.expectOne('http://api.test/api/auth/csrf').flush({ token: 'correct-memory-token' });
    await csrfPromise;

    http.post('http://api.test/api/auth/refresh', {}).subscribe();
    const refresh = httpMock.expectOne('http://api.test/api/auth/refresh');
    expect(refresh.request.headers.get('X-XSRF-TOKEN')).toBe('correct-memory-token');
    expect(refresh.request.headers.get('X-XSRF-TOKEN')).not.toBe('wrong-cookie-value');
    refresh.flush({ accessToken: 'jwt', user: {} });
  });
});
