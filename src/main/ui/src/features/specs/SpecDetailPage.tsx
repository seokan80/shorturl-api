import { useMemo } from "react";
import { useParams, Link } from "react-router-dom";
import { apiSpecs } from "../../data/specs";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "../../components/ui/table";
import { Button } from "../../components/ui/button";

export function SpecDetailPage() {
  const { specId } = useParams();

  const spec = useMemo(
    () => apiSpecs.find((item) => item.id === specId),
    [specId]
  );

  if (!spec) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Spec Not Found</CardTitle>
        </CardHeader>
        <CardContent>
          The requested specification could not be located.{" "}
          <Link to="/specs" className="text-brand hover:underline">
            Return to list.
          </Link>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {spec.category}
          </p>
          <h1 className="mt-2 flex flex-wrap items-center gap-3 text-2xl font-semibold text-slate-900 dark:text-slate-50">
            <span className="rounded-md border border-slate-200 bg-white px-3 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
              {spec.method}
            </span>
            <span className="font-mono text-xl text-slate-900 dark:text-slate-100">
              {spec.path}
            </span>
          </h1>
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
            {spec.summary}
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            <Badge variant="outline">Owner · {spec.owner}</Badge>
            <Badge variant="outline">Version {spec.version}</Badge>
            <Badge
              variant={
                spec.status === "승인됨"
                  ? "success"
                  : spec.status === "검토 대기"
                  ? "warning"
                  : "outline"
              }
            >
              {spec.status}
            </Badge>
            <Badge variant="outline">Auth · {spec.authentication}</Badge>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline">Submit for Approval</Button>
          <Button>Edit Spec</Button>
        </div>
      </div>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="request">Request</TabsTrigger>
          <TabsTrigger value="responses">Responses</TabsTrigger>
          <TabsTrigger value="history">History</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <div className="space-y-4 text-sm text-slate-700 dark:text-slate-200">
            <p>{spec.description}</p>
            <div>
              <h2 className="mb-2 text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                Tags
              </h2>
              <div className="flex flex-wrap gap-2">
                {spec.tags.map((tag) => (
                  <Badge key={tag} variant="outline">
                    {tag}
                  </Badge>
                ))}
              </div>
            </div>
            <div>
              <h2 className="mb-2 text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                Headers
              </h2>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Required</TableHead>
                    <TableHead>Description</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {spec.headers.map((header) => (
                    <TableRow key={header.name}>
                      <TableCell className="font-medium text-slate-100">
                        {header.name}
                        {header.value && (
                          <span className="block text-xs text-slate-400">
                            {header.value}
                          </span>
                        )}
                      </TableCell>
                      <TableCell>{header.required ? "Yes" : "Optional"}</TableCell>
                      <TableCell>{header.description}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="request">
          {spec.requestBody ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Required</TableHead>
                  <TableHead>Description</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {spec.requestBody.map((field) => (
                  <TableRow key={field.name}>
                    <TableCell className="font-medium text-slate-100">
                      {field.name}
                    </TableCell>
                    <TableCell>{field.type}</TableCell>
                    <TableCell>{field.required ? "Yes" : "Optional"}</TableCell>
                    <TableCell>{field.description}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-sm text-slate-300">
              No body payload required for this endpoint.
            </p>
          )}
        </TabsContent>

        <TabsContent value="responses">
          <div className="space-y-4">
            {spec.responses.map((response) => (
              <Card key={response.code} className="border-slate-800/80 bg-slate-900/60">
                <CardHeader className="flex flex-row items-center justify-between">
                  <CardTitle className="text-sm text-slate-100">
                    Status {response.code}
                  </CardTitle>
                  <Badge variant="outline">{response.description}</Badge>
                </CardHeader>
                {response.sample && (
                  <CardContent>
                    <pre className="overflow-x-auto rounded-lg bg-slate-950/70 p-4 text-xs text-slate-300">
                      {response.sample}
                    </pre>
                  </CardContent>
                )}
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="history">
          <div className="space-y-4 text-sm text-slate-300">
            <p>
              Change tracking is synchronised nightly from the Oracle `SPEC_VERSION`
              table. Last updated on {spec.lastUpdated}. All approvals captured in
              audit logs.
            </p>
            <ul className="space-y-2 text-xs text-slate-400">
              <li>• Version {spec.version} awaiting promotion.</li>
              <li>• Previous review comments available in Activity tab.</li>
              <li>• Auto-export to OpenAPI triggered after approval.</li>
            </ul>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
