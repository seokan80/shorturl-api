import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { activityLogs } from "../../data/activity";
import { projects } from "../../data/projects";
import { apiSpecs } from "../../data/specs";
import { workflowItems } from "../../data/workflow";

const metrics = [
  {
    id: "projects",
    label: "Active Projects",
    value: projects.filter((p) => p.status === "Active").length,
    delta: "+1 vs last week"
  },
  {
    id: "drafts",
    label: "Draft Specs",
    value: apiSpecs.filter((spec) => spec.status === "Draft").length,
    delta: "review SLA 48h"
  },
  {
    id: "pending",
    label: "Pending Reviews",
    value: workflowItems.filter((wf) => wf.stage !== "Approve").length,
    delta: "need reviewers"
  },
  {
    id: "overdue",
    label: "Overdue Approvals",
    value: workflowItems.filter(
      (wf) => new Date(wf.dueDate).getTime() < Date.now()
    ).length,
    delta: "notify leads"
  }
];

export function DashboardPage() {
  return (
    <div className="flex flex-col gap-6">
      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {metrics.map((metric) => (
          <Card key={metric.id}>
            <CardHeader>
              <CardTitle>{metric.label}</CardTitle>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                {metric.delta}
              </span>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-semibold text-slate-900 dark:text-slate-50">
                {metric.value}
              </div>
            </CardContent>
          </Card>
        ))}
      </section>

      <section className="grid gap-4 lg:grid-cols-[2fr_1fr]">
        <Card className="h-full">
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              Latest updates synced from Oracle audit log
            </span>
          </CardHeader>
          <CardContent className="space-y-4">
            {activityLogs.map((activity) => (
              <div key={activity.id} className="flex items-center justify-between gap-4">
                <div>
                  <p className="font-medium text-slate-900 dark:text-slate-100">
                    {activity.action}
                  </p>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    {activity.context} ·{" "}
                    {new Date(activity.timestamp).toLocaleString()}
                  </p>
                </div>
                <Badge variant="outline">{activity.actor}</Badge>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Workflow Highlights</CardTitle>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              Triage items requiring attention
            </span>
          </CardHeader>
          <CardContent className="space-y-4">
            {workflowItems.map((item) => {
              const overdue = new Date(item.dueDate).getTime() < Date.now();
              return (
                <div
                  key={item.id}
                  className="rounded-lg border border-slate-200 bg-white/80 p-3 dark:border-slate-800/70 dark:bg-transparent"
                >
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {item.specName}
                    </span>
                    <Badge variant={overdue ? "destructive" : "warning"}>
                      {item.stage}
                    </Badge>
                  </div>
                  <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                    Due {new Date(item.dueDate).toLocaleString()} · Assigned to{" "}
                    {item.assignee}
                  </p>
                </div>
              );
            })}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
