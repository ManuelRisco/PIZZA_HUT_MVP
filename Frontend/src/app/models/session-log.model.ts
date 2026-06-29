export interface SessionLog {
  id: number;
  userId: number;
  userName?: string;
  sessionToken?: string;
  ipAddress?: string;
  userAgent?: string;
  loginTime: string;
  lastActivityTime?: string;
  logoutTime?: string;
  status: 'ACTIVE' | 'CLOSED_MANUAL' | 'CLOSED_TIMEOUT' | 'CLOSED_BY_ADMIN' | string;
}
