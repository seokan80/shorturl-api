import { apiSpecs } from "../../data/specs";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Checkbox } from "../../components/ui/checkbox";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";

const redirectSpec = apiSpecs.find((spec) => spec.id === "redirection-redirect");

export function RedirectionControlsPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>리디렉션 제어</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            공개 리디렉션 엔드포인트({redirectSpec?.path}) 동작을 제어합니다.
          </p>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40">
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">오류 fallback URL</p>
            <Input className="mt-2" defaultValue="https://sho.rt/error" />
          </div>
          <div className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40">
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">추적 필드</p>
            <div className="mt-3 flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
              {[
                "Referer",
                "User-Agent",
                "Country Code",
                "Device Type"
              ].map((label) => (
                <label key={label} className="flex items-center gap-3">
                  <Checkbox defaultChecked aria-label={label} />
                  <span>{label}</span>
                </label>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>응답 정의</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Swagger 명세에 정의된 리디렉션 응답 코드입니다.
          </p>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>응답 코드</TableHead>
                <TableHead>설명</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {redirectSpec?.responses.map((response) => (
                <TableRow key={response.code}>
                  <TableCell className="font-medium">{response.code}</TableCell>
                  <TableCell>{response.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
