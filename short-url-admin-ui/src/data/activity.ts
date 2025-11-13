export interface ActivityLog {
  id: number;
  timestamp: string;
  actor: string;
  action: string;
  context: string;
}

export const activityLogs: ActivityLog[] = [
  {
    id: 1,
    timestamp: "2025-01-13T10:12:00",
    actor: "kim.j",
    action: "approved spec version v1.4",
    context: "Short URL API"
  },
  {
    id: 2,
    timestamp: "2025-01-13T09:47:00",
    actor: "seo.k",
    action: "commented on /api/short-url POST",
    context: "Short URL API"
  },
  {
    id: 3,
    timestamp: "2025-01-12T21:05:00",
    actor: "lee.y",
    action: "uploaded OpenAPI bundle",
    context: "Billing Service"
  }
];
