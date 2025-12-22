import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, RefreshCw, Search, Trash2, Copy, Link as LinkIcon } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { cn } from "../../lib/utils";

type ApiEnvelope<T> = {
  code: string;
  message: string;
  data: T;
};

type ShortUrlItem = {
  id: number;
  shortKey: string;
  shortUrl: string;
  longUrl: string;
  createdBy: string;
  userId: number;
  createdAt: string;
  expiredAt: string | null;
};

type ShortUrlList = {
  totalCount: number;
  elements: ShortUrlItem[];
};

const PAGE_SIZE = 10;
const CLIENT_KEY_STORAGE_KEY = "shorturl:accessKey";

const formatDateTime = (value?: string | null) => (value ? new Date(value).toLocaleString() : "-");
const toInputValue = (value?: string | null) => {
  if (!value) return "";
  return value.length >= 16 ? value.slice(0, 16) : value;
};
const toServerDate = (value: string) => (value.length === 16 ? `${value}:00` : value);
const isExpired = (value?: string | null) => (value ? new Date(value).getTime() < Date.now() : false);

export function ShortUrlManagementPage() {
  const [items, setItems] = useState<ShortUrlItem[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<ShortUrlItem | null>(null);
  const [createForm, setCreateForm] = useState({ longUrl: "" });
  const [editForm, setEditForm] = useState({ expiredAt: "" });
  const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [isFetching, setIsFetching] = useState(false);
  const [clientKey, setClientKey] = useState(() => {
    if (typeof window === "undefined") return "";
    return localStorage.getItem(CLIENT_KEY_STORAGE_KEY) ?? "";
  });

  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (clientKey.trim()) {
      localStorage.setItem(CLIENT_KEY_STORAGE_KEY, clientKey.trim());
    } else {
      localStorage.removeItem(CLIENT_KEY_STORAGE_KEY);
    }
  }, [clientKey]);

  const request = useCallback(
    async <T,>(path: string, init: RequestInit = {}, options?: { requireClientKey?: boolean }): Promise<T> => {
      const key = clientKey.trim();
      if (options?.requireClientKey && !key) {
        throw new Error("먼저 클라이언트 키를 입력해주세요.");
      }

      const headers = new Headers(init.headers);
      if (key) {
        headers.set("X-CLIENTACCESS-KEY", key);
      }
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
    },
    [clientKey]
  );

  const fetchList = useCallback(
    async (pageToLoad: number) => {
      setIsFetching(true);
      try {
        const data = await request<ShortUrlList>(`/api/short-url?page=${pageToLoad}&size=${PAGE_SIZE}&sort=createdAt,desc`);
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

  const handleCreate = async () => {
    if (!createForm.longUrl.trim()) {
      setError("원본 URL을 입력해주세요.");
      return;
    }

    await runWithStatus("create", async () => {
      await request<ShortUrlItem>("/api/short-url", {
        method: "POST",
        body: JSON.stringify({ longUrl: createForm.longUrl.trim() })
      }, { requireClientKey: true });
      setCreateForm({ longUrl: "" });
      setSelected(null);
      setEditForm({ expiredAt: "" });
      setSuccess("단축 URL을 생성했습니다.");
      if (page !== 0) {
        setPage(0);
      } else {
        await fetchList(0);
      }
    });
  };

  const handleSelect = async (item: ShortUrlItem) => {
    await runWithStatus(`detail-${item.id}`, async () => {
      const detail = await request<ShortUrlItem>(`/api/short-url/${item.id}`);
      setSelected(detail);
      setEditForm({ expiredAt: toInputValue(detail.expiredAt) });
      setSuccess(`단축 URL(${detail.shortKey}) 상세를 불러왔습니다.`);
    });
  };

  const handleUpdate = async () => {
    if (!selected) return;
    if (!editForm.expiredAt) {
      setError("만료일을 선택해주세요.");
      return;
    }

    await runWithStatus(`update-${selected.id}`, async () => {
      const payload = await request<ShortUrlItem>(
        `/api/short-url/${selected.id}/expiration`,
        {
          method: "PUT",
          body: JSON.stringify({ expiredAt: toServerDate(editForm.expiredAt) })
        },
        { requireClientKey: true }
      );
      setSelected(payload);
      setEditForm({ expiredAt: toInputValue(payload.expiredAt) });
      setItems((prev) => prev.map((item) => (item.id === payload.id ? payload : item)));
      setSuccess(`단축 URL(${payload.shortKey}) 만료일을 수정했습니다.`);
    });
  };

  const handleDelete = async (item: ShortUrlItem) => {
    if (!window.confirm(`'${item.shortKey}' 단축 URL을 삭제하시겠습니까?`)) {
      return;
    }

    await runWithStatus(`delete-${item.id}`, async () => {
      await request(`/api/short-url/${item.id}`, { method: "DELETE" }, { requireClientKey: true });
      if (selected?.id === item.id) {
        setSelected(null);
        setEditForm({ expiredAt: "" });
      }
      setSuccess(`단축 URL(${item.shortKey})을 삭제했습니다.`);
      if (items.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        await fetchList(page);
      }
    });
  };

  const handleRefresh = () => {
    setStatus(null);
    fetchList(page);
  };

  const handleCopy = (value: string) => {
    navigator.clipboard
      ?.writeText(value)
      .then(() => setSuccess("클립보드에 복사했습니다."))
      .catch(() => setError("복사에 실패했습니다."));
  };

  const disablePrev = page === 0;
  const disableNext = page >= totalPages - 1 || total === 0;
  const hasClientKey = clientKey.trim().length > 0;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">단축 URL 관리</CardTitle>
          <CardDescription>원본 URL을 등록하고 생성된 키를 확인하세요. 생성자만 삭제/수정할 수 있습니다.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2">
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500 dark:text-slate-400">클라이언트 키 *</p>
            <Input
              placeholder="X-CLIENTACCESS-KEY 값"
              value={clientKey}
              onChange={(event) => setClientKey(event.target.value)}
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500 dark:text-slate-400">원본 URL *</p>
            <Input
              placeholder="https://example.com/landing"
              value={createForm.longUrl}
              onChange={(event) => setCreateForm({ longUrl: event.target.value })}
            />
          </div>
          <div className="flex items-end gap-2 md:col-span-2">
            <Button className="flex-1" onClick={handleCreate} disabled={isBusy("create") || !hasClientKey}>
              {isBusy("create") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              단축 URL 생성
            </Button>
            <Button type="button" variant="outline" onClick={() => setCreateForm({ longUrl: "" })}>
              초기화
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <CardTitle className="text-base">단축 URL 목록</CardTitle>
            <CardDescription>페이징 목록에서 생성자, 만료일, 상태를 확인하세요.</CardDescription>
          </div>
          <div className="flex gap-2">
            <Button type="button" variant="outline" size="sm" onClick={handleRefresh} disabled={isFetching}>
              {isFetching && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              목록 새로고침
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-32">Short Key</TableHead>
                  <TableHead>원본 URL</TableHead>
                  <TableHead className="w-32">생성자</TableHead>
                  <TableHead className="w-36">생성일</TableHead>
                  <TableHead className="w-36">만료일</TableHead>
                  <TableHead className="w-24 text-center">상태</TableHead>
                  <TableHead className="w-28 text-center">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isFetching ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-8 text-center text-sm text-slate-500">
                      데이터를 불러오는 중입니다...
                    </TableCell>
                  </TableRow>
                ) : items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-8 text-center text-sm text-slate-500">
                      등록된 단축 URL이 없습니다. 상단 폼에서 새 URL을 생성하세요.
                    </TableCell>
                  </TableRow>
                ) : (
                  items.map((item) => (
                    <TableRow key={item.id} className={cn(selected?.id === item.id && "bg-slate-50 dark:bg-slate-900/20")}>
                      <TableCell className="font-mono text-xs">
                        <a
                          href={item.shortUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:underline dark:text-blue-400"
                        >
                          {item.shortKey}
                        </a>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="truncate text-sm">{item.longUrl}</span>
                          <Button variant="ghost" size="icon" onClick={() => handleCopy(item.longUrl)} aria-label="원본 URL 복사">
                            <Copy className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm text-slate-600 dark:text-slate-300">{item.createdBy}</TableCell>
                      <TableCell className="text-xs text-slate-500">{formatDateTime(item.createdAt)}</TableCell>
                      <TableCell className="text-xs text-slate-500">{formatDateTime(item.expiredAt)}</TableCell>
                      <TableCell className="text-center">
                        <Badge variant={isExpired(item.expiredAt) ? "outline" : "secondary"}>
                          {isExpired(item.expiredAt) ? "만료" : "활성"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="상세 보기"
                            onClick={() => handleSelect(item)}
                            disabled={isBusy(`detail-${item.id}`)}
                          >
                            {isBusy(`detail-${item.id}`) ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-rose-600 hover:text-rose-700"
                            aria-label="삭제"
                            onClick={() => handleDelete(item)}
                            disabled={isBusy(`delete-${item.id}`)}
                          >
                            {isBusy(`delete-${item.id}`) ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <Trash2 className="h-4 w-4" />
                            )}
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
            <div>총 {total.toLocaleString()}건 · 페이지 {total === 0 ? 0 : page + 1}/{totalPages}</div>
            <div className="flex gap-2">
              <Button type="button" variant="outline" size="sm" onClick={() => setPage((prev) => Math.max(prev - 1, 0))} disabled={disablePrev}>
                이전
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPage((prev) => prev + 1)}
                disabled={disableNext}
              >
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
              <CardTitle className="text-base">단축 URL 상세</CardTitle>
              <CardDescription>{selected.shortKey} · 만료일만 수정 가능합니다.</CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setSelected(null)}>
                닫기
              </Button>
              <Button onClick={handleUpdate} disabled={isBusy(`update-${selected.id}`)}>
                {isBusy(`update-${selected.id}`) && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                저장
              </Button>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">단축 URL</p>
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs">{selected.shortUrl}</span>
                <Button variant="ghost" size="icon" onClick={() => handleCopy(selected.shortUrl)} aria-label="단축 URL 복사">
                  <Copy className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => window.open(selected.shortUrl, "_blank", "noopener")}
                  aria-label="새 탭 열기"
                >
                  <LinkIcon className="h-4 w-4" />
                </Button>
              </div>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">원본 URL</p>
              <p className="text-sm break-all text-slate-700 dark:text-slate-200">{selected.longUrl}</p>
            </div>
            <div className="flex flex-col gap-1">
              <p className="text-xs text-slate-500">생성자</p>
              <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{selected.createdBy}</p>
              <p className="text-xs text-slate-500">생성일 · {formatDateTime(selected.createdAt)}</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">만료일</p>
              <Input
                type="datetime-local"
                value={editForm.expiredAt}
                onChange={(event) => setEditForm({ expiredAt: event.target.value })}
              />
              <p className="text-xs text-slate-500">서버에는 yyyy-MM-dd&apos;T&apos;HH:mm:ss으로 전송됩니다.</p>
            </div>
          </CardContent>
        </Card>
      )}

      {status && (
        <div
          className={cn(
            "flex items-center gap-2 rounded-md border px-3 py-2 text-sm",
            status.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-800"
          )}
        >
          <RefreshCw className="h-4 w-4" />
          <span>{status.message}</span>
        </div>
      )}
    </div>
  );
}
