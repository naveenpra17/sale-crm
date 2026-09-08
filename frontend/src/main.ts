import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideServiceWorker } from '@angular/service-worker';
import { appRoutes } from './app/app.routes';
import { AppComponent } from './app/app.component';
import { authInterceptor } from './app/core/interceptors/auth.interceptor';
import { isDevMode, provideAppInitializer, inject } from '@angular/core';
import { AuthService } from './app/core/auth/auth.service';
import { AppConfigService } from './app/core/config/app-config.service';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(appRoutes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideServiceWorker('ngsw-worker.js', { enabled: !isDevMode() }),
    provideAppInitializer(() => inject(AppConfigService).load()),
    provideAppInitializer(() => inject(AuthService).initialize())
  ]
}).catch(console.error);
