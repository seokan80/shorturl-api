export interface WorkflowItem {
  id: number;
  specId: string;
  specName: string;
  stage: "Draft" | "Review" | "Approve";
  assignee: string;
  dueDate: string;
  priority: "Normal" | "High";
}

export const workflowItems: WorkflowItem[] = [
  {
    id: 506,
    specId: "shorturl-create",
    specName: "/api/short-url (POST)",
    stage: "Review",
    assignee: "kim.j",
    dueDate: "2025-01-14T18:00:00",
    priority: "Normal"
  },
  {
    id: 498,
    specId: "auth-register",
    specName: "/api/auth/register (POST)",
    stage: "Approve",
    assignee: "seo.kan",
    dueDate: "2025-01-13T12:00:00",
    priority: "High"
  },
  {
    id: 492,
    specId: "redirection-stats",
    specName: "/r/history/{shortUrlId}/stats (POST)",
    stage: "Review",
    assignee: "lee.y",
    dueDate: "2025-01-15T09:00:00",
    priority: "Normal"
  }
];
