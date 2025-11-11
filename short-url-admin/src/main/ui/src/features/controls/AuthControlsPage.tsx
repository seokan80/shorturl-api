import { apiSpecs } from "../../data/specs";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";

const registerSpec = apiSpecs.find((spec) => spec.id === "auth-register");
const tokenSpec = apiSpecs.find((spec) => spec.id === "auth-token");

export function AuthControlsPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>인증 제어</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Swagger 명세에 정의된 필수 헤더와 응답 구조를 참고해 등록 키를 관리하고 토큰 만료 정책을 조정합니다.
          </p>
        </CardHeader>
        <CardContent className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40">
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">현재 등록 키</p>
            <Input className="mt-2" defaultValue="nh-platform-reg-key" readOnly />
            <div className="mt-3 flex gap-2">
              <Button size="sm">교체</Button>
              <Button size="sm" variant="outline">
                회수
              </Button>
            </div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40">
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">토큰 만료 시간 (분)</p>
            <Input className="mt-2" defaultValue="60" type="number" min={5} max={240} />
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
              재발급 흐름은 {tokenSpec?.path} 명세를 따릅니다.
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>헤더 요구사항</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {registerSpec?.path} 엔드포인트에서 정의된 필수 헤더입니다.
          </p>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>헤더명</TableHead>
                <TableHead>필수 여부</TableHead>
                <TableHead>설명</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {registerSpec?.headers.map((header) => (
                <TableRow key={header.name}>
                  <TableCell className="font-medium">{header.name}</TableCell>
                  <TableCell>{header.required ? "필수" : "선택"}</TableCell>
                  <TableCell>{header.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>샘플 응답</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {registerSpec?.responses.map((response) => (
              <div key={response.code} className="rounded-lg border border-slate-200 p-4 dark:border-slate-800">
                <div className="flex items-center justify-between text-sm">
                  <span className="font-semibold text-slate-800 dark:text-slate-100">
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
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
