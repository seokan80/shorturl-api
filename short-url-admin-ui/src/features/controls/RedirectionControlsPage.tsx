import { useEffect, useState } from "react";
import { Loader2, RefreshCw, Save } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Checkbox } from "../../components/ui/checkbox";
import { cn } from "../../lib/utils";

type RedirectionConfig = {
  fallbackUrl: string;
  defaultHost: string;
  showErrorPage: boolean;
  trackingFields: string;
};

type ApiEnvelope<T> = {
  code: string;
  message: string;
  data: T;
};

export function RedirectionControlsPage() {
  const [config, setConfig] = useState<RedirectionConfig>({
    fallbackUrl: "",
    defaultHost: "",
    showErrorPage: true,
    trackingFields: ""
  });
  const [isFetching, setIsFetching] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const fetchConfig = async () => {
    setIsFetching(true);
    setStatus(null);
    try {
      const response = await fetch("/api/redirection-configs");
      const payload = (await response.json()) as ApiEnvelope<RedirectionConfig>;
      if (payload.code === "0000") {
        setConfig(payload.data);
      }
    } catch (error) {
      console.error(error);
      setStatus({ type: "error", message: "설정을 불러오는데 실패했습니다." });
    } finally {
      setIsFetching(false);
    }
  };

  useEffect(() => {
    fetchConfig();
  }, []);

  const handleUpdate = async () => {
    setIsUpdating(true);
    setStatus(null);
    try {
      const response = await fetch("/api/redirection-configs", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(config)
      });
      const payload = (await response.json()) as ApiEnvelope<RedirectionConfig>;
      if (payload.code === "0000") {
        setConfig(payload.data);
        setStatus({ type: "success", message: "설정이 저장되었습니다." });
      } else {
        setStatus({ type: "error", message: payload.message });
      }
    } catch (error) {
      console.error(error);
      setStatus({ type: "error", message: "설정 저장 중 오류가 발생했습니다." });
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle className="text-lg">리디렉션 제어 설정</CardTitle>
            <CardDescription>리디렉션 실패 시 처리 방식 및 추가 파라미터를 설정합니다.</CardDescription>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={fetchConfig} disabled={isFetching}>
              {isFetching ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
            </Button>
            <Button size="sm" onClick={handleUpdate} disabled={isUpdating}>
              {isUpdating ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
              저장
            </Button>
          </div>
        </CardHeader>
        <CardContent className="grid gap-6">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">오류 Fallback URL</label>
              <Input
                placeholder="https://example.com/error"
                value={config.fallbackUrl ?? ""}
                onChange={(e) => setConfig({ ...config, fallbackUrl: e.target.value })}
              />
              <p className="text-xs text-slate-500">단축 URL을 찾을 수 없거나 만료된 경우 이동할 기본 페이지 주소입니다.</p>
            </div>
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">기본 리다이렉션 호스트 (Root)</label>
              <Input
                placeholder="https://example.com"
                value={config.defaultHost ?? ""}
                onChange={(e) => setConfig({ ...config, defaultHost: e.target.value })}
              />
              <p className="text-xs text-slate-500">단축 키 없이 도메인 루트로 접속했을 때 이동할 주소입니다.</p>
            </div>
          </div>

          <div className="flex flex-col gap-4 border-t pt-6 border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-3">
              <Checkbox
                id="showError"
                checked={config.showErrorPage}
                onCheckedChange={(checked) => setConfig({ ...config, showErrorPage: !!checked })}
              />
              <label htmlFor="showError" className="text-sm font-medium cursor-pointer">실패 사유를 페이지로 노출</label>
            </div>
            <p className="text-xs text-slate-500 ml-7">Fallback URL이 없을 때, 리디렉션 실패 원인을 담은 안내 페이지를 보여줍니다.</p>
          </div>

          <div className="flex flex-col gap-2 border-t pt-6 border-slate-100 dark:border-slate-800">
            <label className="text-sm font-medium">추적 필드 (파라미터 전달)</label>
            <Input
              placeholder="utm_source, utm_medium, utm_campaign"
              value={config.trackingFields ?? ""}
              onChange={(e) => setConfig({ ...config, trackingFields: e.target.value })}
            />
            <p className="text-xs text-slate-500">리디렉션 시 원본 URL로 그대로 전달할 쿼리 파라미터 목록입니다. (쉼표로 구분)</p>
          </div>

          {status && (
            <div
              className={cn(
                "flex items-center gap-2 rounded-md border px-3 py-2 text-sm",
                status.type === "success"
                  ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                  : "border-rose-200 bg-rose-50 text-rose-800"
              )}
            >
              <span>{status.message}</span>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
