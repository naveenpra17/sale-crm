import { inject } from '@angular/core';
import { AppConfigService } from '../config/app-config.service';
import { AuthService } from '../auth/auth.service';

/**
 * Chained application startup: runtime config must finish before auth restore.
 * Exported for unit tests — do not rely on ordering between separate APP_INITIALIZERs.
 */
export async function startupApplication(
  config: AppConfigService,
  auth: AuthService
): Promise<void> {
  await config.load();
  await auth.initialize();
}

/** Single APP_INITIALIZER factory — config.load() always completes before auth.initialize(). */
export function runAppStartup(): () => Promise<void> {
  return () => startupApplication(inject(AppConfigService), inject(AuthService));
}
