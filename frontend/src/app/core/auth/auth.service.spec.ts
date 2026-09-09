import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AppConfigService } from '../config/app-config.service';

const API = 'http://api.test/api';
const USER = {
  id: 1,
  name: 'Admin',
  email: 'admin@example.com',
  role: 'ADMIN' as const,
  active: true,
  mustChangePassword: false
};

describe('AuthService.initialize', () => {
  let auth: AuthService;
  let httpMock: HttpTestingController;
  const configMock = {
    isLoaded: true,
    apiBaseUrl: API,
    load: async () => undefined
  };

  beforeEach(() => {
    configMock.isLoaded = true;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: configMock }
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    auth.initializing = true;
    (auth as unknown as { initialized: boolean }).initialized = false;
  });

  afterEach(() => httpMock.verify());

  it('calls /auth/csrf then /auth/refresh on browser refresh startup', fakeAsync(() => {
    auth.initialize();
    httpMock.expectOne(`${API}/auth/csrf`).flush({ token: 'csrf-token' });
    tick();
    const refresh = httpMock.expectOne(`${API}/auth/refresh`);
    expect(refresh.request.method).toBe('POST');
    expect(refresh.request.withCredentials).toBe(true);
    refresh.flush({ accessToken: 'access-jwt', user: USER });
    tick();

    expect(auth.token).toBe('access-jwt');
    expect(auth.user).toEqual(USER);
    expect(auth.initializing).toBeFalse();
  }));

  it('restores session when refresh cookie is valid', fakeAsync(() => {
    auth.initialize();
    httpMock.expectOne(`${API}/auth/csrf`).flush({ token: 'csrf-token' });
    tick();
    httpMock.expectOne(`${API}/auth/refresh`).flush({ accessToken: 'restored-jwt', user: USER });
    tick();

    expect(auth.token).toBe('restored-jwt');
    expect(auth.user?.email).toBe('admin@example.com');
    expect(auth.initializing).toBeFalse();
  }));

  it('ends unauthenticated when refresh cookie is missing or expired', fakeAsync(() => {
    auth.initialize();
    httpMock.expectOne(`${API}/auth/csrf`).flush({ token: 'csrf-token' });
    tick();
    httpMock
      .expectOne(`${API}/auth/refresh`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    tick();

    expect(auth.token).toBeNull();
    expect(auth.user).toBeNull();
    expect(auth.initializing).toBeFalse();
  }));

  it('does not start before runtime config is loaded', fakeAsync(() => {
    configMock.isLoaded = false;
    let rejected = false;
    auth.initialize().catch(() => (rejected = true));
    tick();

    expect(rejected).toBeTrue();
    httpMock.expectNone(`${API}/auth/csrf`);
    httpMock.expectNone(`${API}/auth/refresh`);
    expect(auth.user).toBeNull();
    expect(auth.initializing).toBeFalse();
  }));
});
