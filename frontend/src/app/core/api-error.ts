/** Extract a user-facing message from an HTTP error (interceptor or API body). */
export function apiErrorMessage(error: unknown, fallback: string): string {
  const e = error as { friendlyMessage?: string; error?: { message?: string } };
  return e?.friendlyMessage || e?.error?.message || fallback;
}
