export interface ProjectSummary {
  id: number;
  name: string;
  owner: string;
  specs: number;
  drafts: number;
  pending: number;
  lastSync: string;
  status: "Active" | "Archived";
}

export const projects: ProjectSummary[] = [
  {
    id: 12,
    name: "Short URL API",
    owner: "seo.k",
    specs: 18,
    drafts: 2,
    pending: 1,
    lastSync: "2025-01-12",
    status: "Active"
  },
  {
    id: 9,
    name: "Billing Service",
    owner: "kim.j",
    specs: 34,
    drafts: 6,
    pending: 3,
    lastSync: "2025-01-10",
    status: "Active"
  },
  {
    id: 6,
    name: "Analytics Gateway",
    owner: "lee.y",
    specs: 12,
    drafts: 4,
    pending: 2,
    lastSync: "2025-01-08",
    status: "Archived"
  }
];
