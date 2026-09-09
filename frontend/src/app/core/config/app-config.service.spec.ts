import { resolveApiBaseUrl } from './app-config.service';

describe('resolveApiBaseUrl', () => {
  const vercel = 'https://sale-crm-vert.vercel.app';

  it('keeps relative /api for same-origin proxy', () => {
    expect(resolveApiBaseUrl('/api', vercel)).toBe('/api');
  });

  it('rewrites cross-origin Render URL to /api proxy', () => {
    expect(resolveApiBaseUrl('https://sale-crm.onrender.com/api', vercel)).toBe('/api');
  });

  it('keeps same-origin absolute URL', () => {
    expect(resolveApiBaseUrl('https://sale-crm-vert.vercel.app/api', vercel)).toBe(
      'https://sale-crm-vert.vercel.app/api'
    );
  });
});
