import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { apiSpecs, specCategories } from "../../data/specs";

export function SpecsOverviewPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>API Specifications</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Categorised view of Oracle `API_SPEC` records with status and owner context.
          </p>
        </CardHeader>
        <CardContent className="space-y-6">
          {specCategories.map((category) => {
            const specsForCategory = apiSpecs.filter(
              (spec) => spec.category === category
            );

            return (
              <section key={category} className="space-y-3">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {category}
                  </h2>
                  <Badge variant="outline">{specsForCategory.length} endpoints</Badge>
                </div>
                <div className="grid gap-4 lg:grid-cols-2">
                  {specsForCategory.map((spec) => (
                    <Card
                      key={spec.id}
                      className="border border-slate-200/70 bg-white/80 transition hover:border-brand/50 dark:border-slate-800/70 dark:bg-slate-900/50"
                    >
                      <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                          <CardTitle className="flex flex-wrap items-center gap-2 text-base text-slate-900 dark:text-slate-50">
                            <span className="rounded-md border border-slate-200 bg-white px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-slate-700 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
                              {spec.method}
                            </span>
                            <span className="font-mono text-sm text-slate-800 dark:text-slate-200">
                              {spec.path}
                            </span>
                          </CardTitle>
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            {spec.summary}
                          </p>
                        </div>
                        <Badge
                          variant={
                            spec.status === "Approved"
                              ? "success"
                              : spec.status === "Pending Review"
                              ? "warning"
                              : "outline"
                          }
                        >
                          {spec.status}
                        </Badge>
                      </CardHeader>
                      <CardContent className="flex flex-col gap-3">
                        <div className="flex flex-wrap gap-2 text-xs">
                          {spec.tags.map((tag) => (
                            <Badge key={tag} variant="outline">
                              {tag}
                            </Badge>
                          ))}
                        </div>
                        <div className="flex items-center justify-between text-xs text-slate-400">
                          <span>Owner · {spec.owner}</span>
                          <span>Last updated · {spec.lastUpdated}</span>
                        </div>
                        <Link
                          to={`/specs/${spec.id}`}
                          className="text-sm font-medium text-brand hover:underline"
                        >
                          View detail →
                        </Link>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </section>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}
