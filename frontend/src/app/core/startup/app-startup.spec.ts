import { AppConfigService } from '../config/app-config.service';
import { AuthService } from '../auth/auth.service';
import { startupApplication } from './app-startup';

describe('startupApplication', () => {
  it('loads runtime config before auth initialization', async () => {
    const order: string[] = [];
    const config = {
      load: jasmine.createSpy('load').and.callFake(async () => {
        order.push('config.load');
      }),
      isLoaded: true,
      apiBaseUrl: 'http://api.test/api'
    } as unknown as AppConfigService;
    const auth = {
      initialize: jasmine.createSpy('initialize').and.callFake(async () => {
        order.push('auth.initialize');
      })
    } as unknown as AuthService;

    await startupApplication(config, auth);

    expect(config.load).toHaveBeenCalledTimes(1);
    expect(auth.initialize).toHaveBeenCalledTimes(1);
    expect(order).toEqual(['config.load', 'auth.initialize']);
  });
});
