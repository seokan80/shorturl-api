import { apiSpecs } from "../../data/specs";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Checkbox } from "../../components/ui/checkbox";

const analyticsSpec = apiSpecs.find((spec) => spec.id === "redirection-stats");

export function AnalyticsControlsPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>통계 제어</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {analyticsSpec?.path} 명세에 정의된 groupBy 파라미터를 On/Off 할 수 있습니다.
          </p>
        </CardHeader>
        <CardContent className="grid gap-3 text-sm text-slate-600 dark:text-slate-300">
          {analyticsSpec?.requestBody?.[0]?.description && (
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {analyticsSpec.requestBody[0].description}
            </p>
          )}
          {[
            "REFERER",
            "YEAR",
            "MONTH",
            "DAY",
            "USER_AGENT",
            "COUNTRY"
          ].map((label, idx) => (
            <label key={label} className="flex items-center gap-3">
              <Checkbox defaultChecked={idx < 4} aria-label={label} />
              <span>{label}</span>
            </label>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>샘플 응답</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Swagger 명세의 응답 예시를 기반으로 UI에서 바로 확인할 수 있습니다.
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          {analyticsSpec?.responses.map((response) => (
            <div key={response.code} className="rounded-lg border border-slate-200 p-4 dark:border-slate-800">
              <div className="flex items-center justify-between text-sm">
                <span className="font-semibold text-slate-900 dark:text-slate-100">
                  Status {response.code}
                </span>
                <Badge variant="outline">{response.description}</Badge>
              </div>
              {response.sample && (
                <pre className="mt-3 overflow-x-auto rounded bg-slate-950/70 p-3 text-xs text-slate-100">
                  {response.sample}
                </pre>
              )}
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
