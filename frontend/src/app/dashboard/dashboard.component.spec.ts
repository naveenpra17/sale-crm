import { TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../core/auth/auth.service';
import { of } from 'rxjs';

describe('DashboardComponent', () => {
  it('caps visual progress at 100%', () => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: DashboardService, useValue: { get: () => of(null) } },
        { provide: AuthService, useValue: { user: { name: 'Ravi' } } }
      ]
    });
    const c = TestBed.createComponent(DashboardComponent).componentInstance;
    c.d = {
      projectName: 'Challenge',
      timezone: 'Asia/Kolkata',
      totalAcres: 50,
      soldAcres: 52,
      remainingAcres: 0,
      progressPercent: 104,
      deadline: new Date(Date.now() + 3600000).toISOString(),
      serverTime: new Date().toISOString(),
      secondsUntilDeadline: 3600,
      daysElapsed: 1,
      daysRemaining: 1,
      currentDailyRate: 1,
      requiredDailyRate: 1,
      paceDifference: 0,
      status: 'TARGET_REACHED',
      leaderboard: [],
      myPerformance: { userId: 1, rank: 1, acresSold: 52, percentageOfTarget: 104, salesCount: 1, dailyRate: 1 },
      recentSales: []
    };
    c.lastUpdated = Date.now();
    c.tick();
    expect(c.visualProgress).toBe(100);
  });

  it('shows challenge complete when deadline passed', () => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: DashboardService, useValue: { get: () => of(null) } },
        { provide: AuthService, useValue: { user: { name: 'Ravi' } } }
      ]
    });
    const c = TestBed.createComponent(DashboardComponent).componentInstance;
    c.d = {
      projectName: 'Challenge',
      timezone: 'Asia/Kolkata',
      totalAcres: 50,
      soldAcres: 10,
      remainingAcres: 40,
      progressPercent: 20,
      deadline: new Date(Date.now() - 1000).toISOString(),
      serverTime: new Date().toISOString(),
      secondsUntilDeadline: 0,
      daysElapsed: 1,
      daysRemaining: 0,
      currentDailyRate: 1,
      requiredDailyRate: 1,
      paceDifference: 0,
      status: 'DEADLINE_PASSED',
      leaderboard: [],
      myPerformance: { userId: 1, rank: 1, acresSold: 10, percentageOfTarget: 20, salesCount: 1, dailyRate: 1 },
      recentSales: []
    };
    c.lastUpdated = Date.now();
    c.tick();
    expect(c.complete).toBeTrue();
    expect(c.days).toBe(0);
  });
});
