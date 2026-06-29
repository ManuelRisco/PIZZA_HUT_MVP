export interface AuditLog {
  id: number;
  userId?: number;
  userName?: string;
  actionType: string;
  entityType?: string;
  entityId?: number;
  ipAddress?: string;
  userAgent?: string;
  requestMethod?: string;
  requestUrl?: string;
  oldValues?: string;
  newValues?: string;
  description?: string;
  status?: string;
  createdAt: string;
}
