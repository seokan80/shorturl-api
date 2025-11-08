import { useMemo } from "react";
import { projects } from "../../data/projects";
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
import { Input } from "../../components/ui/input";

export function ProjectsPage() {
  const activeCount = useMemo(
    () => projects.filter((project) => project.status === "Active").length,
    []
  );

  const archivedCount = projects.length - activeCount;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Project Overview</CardTitle>
          <p className="text-xs text-slate-400">
            Oracle `PROJECT` table snapshot · Active: {activeCount} · Archived:{" "}
            {archivedCount}
          </p>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="grid gap-4 md:grid-cols-3">
            <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
              <p className="text-sm text-slate-400">Total Specs</p>
              <p className="mt-2 text-2xl font-semibold text-slate-50">
                {projects.reduce((acc, project) => acc + project.specs, 0)}
              </p>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
              <p className="text-sm text-slate-400">Drafts In Progress</p>
              <p className="mt-2 text-2xl font-semibold text-slate-50">
                {projects.reduce((acc, project) => acc + project.drafts, 0)}
              </p>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
              <p className="text-sm text-slate-400">Pending Approvals</p>
              <p className="mt-2 text-2xl font-semibold text-slate-50">
                {projects.reduce((acc, project) => acc + project.pending, 0)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <Input placeholder="Filter by project name..." className="max-w-xs" />
            <Badge variant="outline">Total {projects.length}</Badge>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Owner</TableHead>
                <TableHead>Specs</TableHead>
                <TableHead>Drafts</TableHead>
                <TableHead>Pending</TableHead>
                <TableHead>Last Sync</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {projects.map((project) => (
                <TableRow key={project.id}>
                  <TableCell>{project.id}</TableCell>
                  <TableCell className="font-medium text-slate-100">
                    {project.name}
                  </TableCell>
                    <TableCell>{project.owner}</TableCell>
                  <TableCell>{project.specs}</TableCell>
                  <TableCell>{project.drafts}</TableCell>
                  <TableCell>{project.pending}</TableCell>
                  <TableCell>{project.lastSync}</TableCell>
                  <TableCell>
                    <Badge
                      variant={project.status === "Active" ? "success" : "outline"}
                    >
                      {project.status}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
