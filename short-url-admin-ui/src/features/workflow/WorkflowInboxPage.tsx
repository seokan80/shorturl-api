import { workflowItems } from "../../data/workflow";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";

export function WorkflowInboxPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Workflow Inbox</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Items pulled from Oracle `WORKFLOW_ITEM` table awaiting action.
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-400">
            <Badge variant="outline">
              Total {workflowItems.length}
            </Badge>
            <Badge variant="warning">
              High Priority{" "}
              {
                workflowItems.filter((item) => item.priority === "High").length
              }
            </Badge>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Spec</TableHead>
                <TableHead>Stage</TableHead>
                <TableHead>Assignee</TableHead>
                <TableHead>Due</TableHead>
                <TableHead>Priority</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {workflowItems.map((item) => {
                const overdue = new Date(item.dueDate).getTime() < Date.now();
                return (
                  <TableRow key={item.id}>
                    <TableCell>{item.id}</TableCell>
                    <TableCell className="font-medium text-slate-900 dark:text-slate-100">
                      {item.specName}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{item.stage}</Badge>
                    </TableCell>
                    <TableCell>{item.assignee}</TableCell>
                    <TableCell>
                      {new Date(item.dueDate).toLocaleString()}
                      {overdue && (
                        <span className="ml-2 rounded bg-rose-500/10 px-2 py-0.5 text-xs text-rose-600 dark:bg-rose-600/20 dark:text-rose-200">
                          overdue
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={item.priority === "High" ? "destructive" : "outline"}
                      >
                        {item.priority}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline">
                          Reassign
                        </Button>
                        <Button size="sm">Approve</Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
