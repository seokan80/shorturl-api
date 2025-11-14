import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, RefreshCw, Search, Filter } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { cn } from "../../lib/utils";

type ApiEnvelope<T> = {
  code: string;
  message: string;
  data: T;
};

type RedirectionHistoryItem = {
  id: number;
  shortUrlId: number;
  shortUrlKey: string;
  referer: string | null;
  userAgent: string | null;
  ip: string | null;
  redirectAt: string;
};

type RedirectionHistoryList = {
  totalCount: number;
  elements: RedirectionHistoryItem[];
};

const PAGE_SIZE = 10;

const formatDateTime = (value?: string | null) => (value ? new Date(value).toLocaleString() : "-");
const truncate = (value: string | null, maxLength: number = 40) => {
  if (!value) return "-";
  return value.length > maxLength ? `${value.substring(0, maxLength)}...` : value;
};

export function RedirectionHistoryPage() {
  const [items, setItems] = useState<RedirectionHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<RedirectionHistoryItem | null>(null);
  const [searchKey, setSearchKey] = useState("");
  const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [isFetching, setIsFetching] = useState(false);

  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);

  const request = useCallback(async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
    const headers = new Headers(init.headers);
    if (init.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    const response = await fetch(path, { ...init, headers });
    const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;

    if (!payload) {
      throw new Error("응답을 파싱할 수 없습니다.");
    }

    if (!response.ok || payload.code !== "0000") {
      throw new Error(payload.message ?? "요청에 실패했습니다.");
    }

    return payload.data;
  }, []);

  const fetchList = useCallback(
    async (pageToLoad: number) => {
      setIsFetching(true);
      try {
        const data = await request<RedirectionHistoryList>(
          `/api/internal/redirection-histories?page=${pageToLoad}&size=${PAGE_SIZE}&sort=redirectAt,desc`
        );
        setItems(data?.elements ?? []);
        setTotal(data?.totalCount ?? 0);
      } catch (error) {
        const message = error instanceof Error ? error.message : "목록을 불러오는 중 오류가 발생했습니다.";
        setStatus({ type: "error", message });
      } finally {
        setIsFetching(false);
      }
    },
    [request]
  );

  useEffect(() => {
    fetchList(page);
  }, [fetchList, page]);

  const setSuccess = (message: string) => setStatus({ type: "success", message });
  const setError = (message: string) => setStatus({ type: "error", message });
  const isBusy = (label: string) => busyAction === label;

  const runWithStatus = async (label: string, fn: () => Promise<void>) => {
    setBusyAction(label);
    setStatus(null);
    try {
      await fn();
    } catch (error) {
      const message = error instanceof Error ? error.message : "요청 중 오류가 발생했습니다.";
      setError(message);
    } finally {
      setBusyAction(null);
    }
  };

  const handleSelect = async (item: RedirectionHistoryItem) => {
    await runWithStatus(`detail-${item.id}`, async () => {
      const detail = await request<RedirectionHistoryItem>(`/api/internal/redirection-histories/${item.id}`);
      setSelected(detail);
      setSuccess(`리다이렉션 히스토리(ID: ${detail.id}) 상세를 불러왔습니다.`);
    });
  };

  const handleRefresh = () => {
    setStatus(null);
    fetchList(page);
  };

  const filteredItems = useMemo(() => {
    if (!searchKey.trim()) return items;
    const lowerSearch = searchKey.toLowerCase();
    return items.filter(
      (item) =>
        item.shortUrlKey.toLowerCase().includes(lowerSearch) ||
        item.referer?.toLowerCase().includes(lowerSearch) ||
        item.ip?.toLowerCase().includes(lowerSearch)
    );
  }, [items, searchKey]);

  const disablePrev = page === 0;
  const disableNext = page >= totalPages - 1 || total === 0;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">리다이렉션 히스토리</CardTitle>
          <CardDescription>단축 URL 리다이렉션 기록을 조회하고 분석하세요.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <Input
                placeholder="Short URL 키, Referer, IP로 필터링..."
                value={searchKey}
                onChange={(event) => setSearchKey(event.target.value)}
                className="pl-10"
              />
            </div>
            <Button type="button" variant="outline" onClick={handleRefresh} disabled={isFetching}>
              {isFetching ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle className="text-base">리다이렉션 기록 목록</CardTitle>
            <CardDescription>페이징 목록에서 리다이렉션 정보를 확인하세요.</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16">ID</TableHead>
                  <TableHead className="w-32">Short URL 키</TableHead>
                  <TableHead>Referer</TableHead>
                  <TableHead>User Agent</TableHead>
                  <TableHead className="w-32">IP</TableHead>
                  <TableHead className="w-40">리다이렉션 일시</TableHead>
                  <TableHead className="w-20 text-center">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isFetching ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-8 text-center text-sm text-slate-500">
                      데이터를 불러오는 중입니다...
                    </TableCell>
                  </TableRow>
                ) : filteredItems.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-8 text-center text-sm text-slate-500">
                      {searchKey.trim() ? "검색 결과가 없습니다." : "리다이렉션 기록이 없습니다."}
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredItems.map((item) => (
                    <TableRow key={item.id} className={cn(selected?.id === item.id && "bg-slate-50 dark:bg-slate-900/20")}>
                      <TableCell className="font-mono text-xs">{item.id}</TableCell>
                      <TableCell className="font-mono text-xs">{item.shortUrlKey}</TableCell>
                      <TableCell className="text-sm" title={item.referer || "-"}>
                        {truncate(item.referer)}
                      </TableCell>
                      <TableCell className="text-sm" title={item.userAgent || "-"}>
                        {truncate(item.userAgent)}
                      </TableCell>
                      <TableCell className="font-mono text-xs">{item.ip || "-"}</TableCell>
                      <TableCell className="text-xs text-slate-500">{formatDateTime(item.redirectAt)}</TableCell>
                      <TableCell>
                        <div className="flex justify-center">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="상세 보기"
                            onClick={() => handleSelect(item)}
                            disabled={isBusy(`detail-${item.id}`)}
                          >
                            {isBusy(`detail-${item.id}`) ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
          <div className="flex flex-col items-center gap-2 text-sm text-slate-600 dark:text-slate-300 md:flex-row md:justify-between">
            <div>
              총 {total.toLocaleString()}건 · 페이지 {total === 0 ? 0 : page + 1}/{totalPages}
              {searchKey.trim() && ` · 필터링된 결과: ${filteredItems.length}건`}
            </div>
            <div className="flex gap-2">
              <Button type="button" variant="outline" size="sm" onClick={() => setPage((prev) => Math.max(prev - 1, 0))} disabled={disablePrev}>
                이전
              </Button>
              <Button type="button" variant="outline" size="sm" onClick={() => setPage((prev) => prev + 1)} disabled={disableNext}>
                다음
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {selected && (
        <Card>
          <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <CardTitle className="text-base">리다이렉션 히스토리 상세</CardTitle>
              <CardDescription>ID: {selected.id} · Short URL: {selected.shortUrlKey}</CardDescription>
            </div>
            <Button variant="outline" onClick={() => setSelected(null)}>
              닫기
            </Button>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">ID</p>
              <p className="font-mono text-sm text-slate-700 dark:text-slate-200">{selected.id}</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">Short URL ID</p>
              <p className="font-mono text-sm text-slate-700 dark:text-slate-200">{selected.shortUrlId}</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">Short URL 키</p>
              <p className="font-mono text-sm text-slate-700 dark:text-slate-200">{selected.shortUrlKey}</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">IP 주소</p>
              <p className="font-mono text-sm text-slate-700 dark:text-slate-200">{selected.ip || "-"}</p>
            </div>
            <div className="flex flex-col gap-2 md:col-span-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">Referer</p>
              <p className="break-all text-sm text-slate-700 dark:text-slate-200">{selected.referer || "-"}</p>
            </div>
            <div className="flex flex-col gap-2 md:col-span-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">User Agent</p>
              <p className="break-all text-sm text-slate-700 dark:text-slate-200">{selected.userAgent || "-"}</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">리다이렉션 일시</p>
              <p className="text-sm text-slate-700 dark:text-slate-200">{formatDateTime(selected.redirectAt)}</p>
            </div>
          </CardContent>
        </Card>
      )}

      {status && (
        <div
          className={cn(
            "flex items-center gap-2 rounded-md border px-3 py-2 text-sm",
            status.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200"
              : "border-rose-200 bg-rose-50 text-rose-800 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-200"
          )}
        >
          <RefreshCw className="h-4 w-4" />
          <span>{status.message}</span>
        </div>
      )}
    </div>
  );
}
