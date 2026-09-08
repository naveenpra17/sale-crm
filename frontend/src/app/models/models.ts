export interface User {
  id: number;
  name: string;
  email: string;
  role: 'ADMIN' | 'USER';
  active: boolean;
  mustChangePassword: boolean;
  lastLoginAt?: string;
}

export interface Sale {
  id: number;
  userId: number;
  salesperson: string;
  acres: number;
  saleDate: string;
  buyerName?: string;
  plotReference?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LeaderboardItem {
  rank: number;
  userId: number;
  name: string;
  acresSold: number;
  percentageOfTeamSales: number;
}

export interface LeaderboardResponse {
  entries: LeaderboardItem[];
  currentUser: LeaderboardItem;
  teamTotalAcres: number;
}

export interface MyPerformance {
  userId: number;
  rank: number;
  acresSold: number;
  percentageOfTarget: number;
  salesCount: number;
  dailyRate: number;
}

export interface Dashboard {
  projectName: string;
  timezone: string;
  totalAcres: number;
  soldAcres: number;
  remainingAcres: number;
  progressPercent: number;
  deadline: string;
  serverTime: string;
  secondsUntilDeadline: number;
  daysElapsed: number;
  daysRemaining: number;
  currentDailyRate: number;
  requiredDailyRate: number;
  paceDifference: number;
  projectedCompletionDate?: string;
  status: string;
  leaderboard: LeaderboardItem[];
  myPerformance: MyPerformance;
  recentSales: Sale[];
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  last: boolean;
}
