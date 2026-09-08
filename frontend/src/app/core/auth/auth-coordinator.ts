import { User } from '../../models/models';

export interface RefreshPayload {
  accessToken: string;
  user: User;
}

const CHANNEL = 'acres-auth';
const LOCK_NAME = 'acres-auth-refresh';
const LEADER_DELAY_MS = 60;
const WAIT_TIMEOUT_MS = 15000;

export class AuthCoordinator {
  private static channel: BroadcastChannel | null =
    typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel(CHANNEL) : null;

  /**
   * Only one tab calls the refresh API. Other tabs wait for the leader's result
   * via BroadcastChannel and update in-memory auth state without a second refresh.
   */
  static async coordinateRefresh(leaderRefresh: () => Promise<RefreshPayload>): Promise<RefreshPayload> {
    if (typeof navigator !== 'undefined' && 'locks' in navigator) {
      return this.withWebLock(leaderRefresh);
    }
    if (this.channel) {
      return this.withBroadcastChannel(leaderRefresh);
    }
    return leaderRefresh();
  }

  private static async withWebLock(leaderRefresh: () => Promise<RefreshPayload>): Promise<RefreshPayload> {
    let leaderPayload: RefreshPayload | null = null;
    const wasLeader = await navigator.locks.request(LOCK_NAME, { ifAvailable: true }, async lock => {
      if (!lock) {
        return false;
      }
      leaderPayload = await leaderRefresh();
      this.channel?.postMessage({ type: 'refresh-complete', payload: leaderPayload });
      return true;
    });

    if (wasLeader && leaderPayload) {
      return leaderPayload;
    }

    try {
      return await this.waitForPeerRefresh();
    } catch {
      return navigator.locks.request(LOCK_NAME, async () => {
        const payload = await leaderRefresh();
        this.channel?.postMessage({ type: 'refresh-complete', payload });
        return payload;
      });
    }
  }

  private static withBroadcastChannel(leaderRefresh: () => Promise<RefreshPayload>): Promise<RefreshPayload> {
    const tabId = crypto.randomUUID();
    return new Promise((resolve, reject) => {
      let settled = false;
      let leaderTimer: ReturnType<typeof setTimeout> | undefined;

      const finish = (payload: RefreshPayload) => {
        if (settled) return;
        settled = true;
        cleanup();
        resolve(payload);
      };

      const fail = (err: unknown) => {
        if (settled) return;
        settled = true;
        cleanup();
        reject(err);
      };

      const onMessage = (event: MessageEvent) => {
        if (event.data?.type === 'refresh-leading' && event.data.tabId !== tabId && leaderTimer) {
          clearTimeout(leaderTimer);
          leaderTimer = undefined;
        }
        if (event.data?.type === 'refresh-complete' && event.data.payload) {
          finish(event.data.payload as RefreshPayload);
        }
        if (event.data?.type === 'refresh-failed') {
          fail(new Error('Refresh failed in another tab'));
        }
      };

      const waitTimer = setTimeout(() => fail(new Error('Refresh timed out')), WAIT_TIMEOUT_MS);

      const cleanup = () => {
        clearTimeout(waitTimer);
        if (leaderTimer) clearTimeout(leaderTimer);
        this.channel?.removeEventListener('message', onMessage);
      };

      this.channel!.addEventListener('message', onMessage);
      this.channel!.postMessage({ type: 'refresh-request', tabId });

      leaderTimer = setTimeout(async () => {
        if (settled) return;
        this.channel!.postMessage({ type: 'refresh-leading', tabId });
        try {
          const payload = await leaderRefresh();
          this.channel!.postMessage({ type: 'refresh-complete', payload });
          finish(payload);
        } catch (e) {
          this.channel!.postMessage({ type: 'refresh-failed' });
          fail(e);
        }
      }, LEADER_DELAY_MS);
    });
  }

  private static waitForPeerRefresh(): Promise<RefreshPayload> {
    return new Promise((resolve, reject) => {
      if (!this.channel) {
        reject(new Error('BroadcastChannel unavailable'));
        return;
      }
      const timer = setTimeout(() => {
        cleanup();
        reject(new Error('Timed out waiting for peer tab refresh'));
      }, WAIT_TIMEOUT_MS);

      const onMessage = (event: MessageEvent) => {
        if (event.data?.type === 'refresh-complete' && event.data.payload) {
          cleanup();
          resolve(event.data.payload as RefreshPayload);
        }
        if (event.data?.type === 'refresh-failed') {
          cleanup();
          reject(new Error('Peer tab refresh failed'));
        }
      };

      const cleanup = () => {
        clearTimeout(timer);
        this.channel?.removeEventListener('message', onMessage);
      };

      this.channel.addEventListener('message', onMessage);
    });
  }
}
