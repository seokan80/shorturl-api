import { apiSpecs } from "../../data/specs";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";

const createSpec = apiSpecs.find((spec) => spec.id === "shorturl-create");
const detailSpec = apiSpecs.find((spec) => spec.id === "shorturl-detail");

export function ShortUrlControlsPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>단축 URL 제어</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Swagger 명세에서 정의된 요구사항을 기반으로 만료 시간과 Short Key 생성을 제어합니다.
          </p>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40">
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">Key 길이</p>
            <Input className="mt-2" defaultValue="8" type="number" min={4} max={12} />
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {createSpec?.summary}
            </p>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40">
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">기본 만료 (시간)</p>
            <Input className="mt-2" defaultValue="24" type="number" min={1} max={168} />
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              JWT 인증 필요 · {createSpec?.authentication}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>엔드포인트 참고</CardTitle>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            생성({createSpec?.path})과 조회({detailSpec?.path}) 명세 요약입니다.
          </p>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          {[createSpec, detailSpec].map((spec) => (
            <div
              key={spec?.id}
              className="rounded-lg border border-slate-200 bg-white/80 p-4 dark:border-slate-800 dark:bg-slate-950/40"
            >
              <div className="flex items-center gap-2 text-sm">
                <Badge variant="outline">{spec?.method}</Badge>
                <span className="font-mono text-xs">{spec?.path}</span>
              </div>
              <p className="mt-2 text-sm font-semibold text-slate-900 dark:text-slate-100">{spec?.name}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">{spec?.summary}</p>
              <Button className="mt-3" size="sm" variant="secondary">
                스키마 보기
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
