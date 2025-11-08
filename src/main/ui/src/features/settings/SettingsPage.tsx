import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Checkbox } from "../../components/ui/checkbox";

export function SettingsPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Integrations</CardTitle>
          <p className="text-xs text-slate-400">
            Manage external services tied to CMS updates.
          </p>
        </CardHeader>
        <CardContent className="space-y-4 text-sm text-slate-300">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
              <h3 className="font-semibold text-slate-100">Git Repository</h3>
              <p className="mt-1 text-xs text-slate-400">
                Configure repository for automated OpenAPI exports.
              </p>
              <Input className="mt-3" defaultValue="git@github.com:team/shorturl-api.git" />
              <Button className="mt-3" size="sm">
                Update
              </Button>
            </div>
            <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
              <h3 className="font-semibold text-slate-100">Slack Notifications</h3>
              <p className="mt-1 text-xs text-slate-400">
                Channel for workflow alerts and approvals.
              </p>
              <Input className="mt-3" defaultValue="#api-cms-alerts" />
              <Button className="mt-3" size="sm">
                Update
              </Button>
            </div>
          </div>
          <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
            <h3 className="font-semibold text-slate-100">Oracle Data Sync</h3>
            <p className="mt-1 text-xs text-slate-400">
              Last refresh: 2025-01-13 08:30 KST · Connection: PROD-ORACLE-01
            </p>
            <div className="mt-3 flex flex-wrap gap-3 text-xs">
              <Badge variant="success">SPEC_VERSION</Badge>
              <Badge variant="success">WORKFLOW_ITEM</Badge>
              <Badge variant="outline">COMMENT (stale)</Badge>
            </div>
            <div className="mt-4 flex items-center gap-3">
              <Checkbox checked title="Enable incremental sync" />
              <Checkbox checked title="Enforce audit log retention (90d)" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
