import { activityLogs } from "../../data/activity";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Textarea } from "../../components/ui/textarea";
import { Button } from "../../components/ui/button";

export function ActivityPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Comment Threads</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Pulls from Oracle `COMMENT` and `ACTIVITY_LOG` tables to retain decision history.
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-3">
            {activityLogs.map((log) => (
              <div
                key={log.id}
                className="rounded-lg border border-slate-200 bg-white/90 p-4 dark:border-slate-800 dark:bg-slate-900/60"
              >
                <div className="flex items-center justify-between text-sm">
                  <span className="font-medium text-slate-900 dark:text-slate-100">
                    {log.actor} · {log.context}
                  </span>
                  <Badge variant="outline">
                    {new Date(log.timestamp).toLocaleString()}
                  </Badge>
                </div>
                <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">{log.action}</p>
              </div>
            ))}
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950/60">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Add Comment</h3>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              Comments are stored in Oracle `COMMENT` table with full audit metadata.
            </p>
            <Textarea className="mt-3" placeholder="Share feedback or attach links..." />
            <div className="mt-3 flex justify-end gap-2">
              <Button variant="outline" size="sm">
                Cancel
              </Button>
              <Button size="sm">Submit</Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
