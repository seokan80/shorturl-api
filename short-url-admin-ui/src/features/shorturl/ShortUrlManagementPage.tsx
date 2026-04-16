import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, RefreshCw, Search, Trash2, Copy, Link as LinkIcon, ExternalLink } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { cn } from "../../lib/utils";
import { apiRequest } from "../../lib/apiClient";
import { formatDateTime, isExpired, toInputDateTime, toServerDateTime } from "../../lib/formats";

type ShortUrlItem = {
  id: number;
  shortKey: string;
  shortUrl: string;
  longUrl: string;
  createdAt: string;
  expiredAt: string | null;
};

type ShortUrlList = {
  totalCount: number;
  elements: ShortUrlItem[];
};

type ExpireMode = "none" | "validDays" | "expireDate";

type CreateForm = {
  longUrl: string;
  mode: ExpireMode;
  validDays: string;
  expireDate: string;
};

const PAGE_SIZE = 10;

const INITIAL_CREATE_FORM: CreateForm = {
  longUrl: "",
  mode: "none",
  validDays: "7",
  expireDate: ""
};

export function ShortUrlManagementPage() {
  const [items, setItems] = useState<ShortUrlItem[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<ShortUrlItem | null>(null);
  const [createForm, setCreateForm] = useState<CreateForm>(INITIAL_CREATE_FORM);
  const [editForm, setEditForm] = useState({ expiredAt: "" });
  const [searchKey, setSearchKey] = useState("");
  const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [isFetching, setIsFetching] = useState(false);

  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);

  const fetchList = useCallback(async (pageToLoad: number) => {
    setIsFetching(true);
    try {
      const data = await apiRequest<ShortUrlList>(
        `/api/short-url?page=${pageToLoad}&size=${PAGE_SIZE}&sort=createdAt,desc`
      );
      setItems(data?.elements ?? []);
      setTotal(data?.totalCount ?? 0);
    } catch (error) {
      const message = error instanceof Error ? error.message : "목록을 불러오는 중 오류가 발생했습니다.";
      setStatus({ type: "error", message });
    } finally {
      setIsFetching(false);
    }
  }, []);

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
      setError("원본 URL 을 입력해주세요.");
      return;
    }

    const body: Record<string, unknown> = { longUrl: createForm.longUrl.trim() };
    if (createForm.mode === "validDays") {
      const n = Number(createForm.validDays);
      if (!Number.isInteger(n) || n <= 0) {
        setError("유효 기간(일) 은 1 이상의 정수여야 합니다.");
        return;
      }
      body.validDays = n;
    } else if (createForm.mode === "expireDate") {
      if (!createForm.expireDate) {
        setError("만료 일시를 선택해주세요.");
        return;
      }
      body.expireDate = toServerDateTime(createForm.expireDate);
    }

    await runWithStatus("create", async () => {
      const created = await apiRequest<ShortUrlItem>("/api/short-url", {
        method: "POST",
        body: JSON.stringify(body)
      });
      setCreateForm(INITIAL_CREATE_FORM);
      setSelected(created);
      setEditForm({ expiredAt: toInputDateTime(created.expiredAt) });
      setSuccess(`단축 URL(${created.shortKey}) 을 생성했습니다.`);
      if (page !== 0) setPage(0);
      else await fetchList(0);
    });
  };

  const handleSelect = async (item: ShortUrlItem) => {
    await runWithStatus(`detail-${item.id}`, async () => {
      const detail = await apiRequest<ShortUrlItem>(`/api/short-url/${item.id}`);
      setSelected(detail);
      setEditForm({ expiredAt: toInputDateTime(detail.expiredAt) });
      setSuccess(`단축 URL(${detail.shortKey}) 상세를 불러왔습니다.`);
    });
  };

  const handleSearchByKey = async () => {
    const key = searchKey.trim();
    if (!key) {
      setError("조회할 Short Key 를 입력해주세요.");
      return;
    }
    await runWithStatus("search-key", async () => {
      const detail = await apiRequest<ShortUrlItem>(`/api/short-url/key/${encodeURIComponent(key)}`);
      setSelected(detail);
      setEditForm({ expiredAt: toInputDateTime(detail.expiredAt) });
      setSuccess(`Short Key(${detail.shortKey}) 조회 성공`);
    });
  };

  const handleUpdate = async () => {
    if (!selected) return;
    if (!editForm.expiredAt) {
      setError("만료일을 선택해주세요.");
      return;
    }
    await runWithStatus(`update-${selected.id}`, async () => {
      const payload = await apiRequest<ShortUrlItem>(`/api/short-url/${selected.id}/expiration`, {
        method: "PUT",
        body: JSON.stringify({ expiredAt: toServerDateTime(editForm.expiredAt) })
      });
      setSelected(payload);
      setEditForm({ expiredAt: toInputDateTime(payload.expiredAt) });
      setItems((prev) => prev.map((it) => (it.id === payload.id ? payload : it)));
      setSuccess(`단축 URL(${payload.shortKey}) 만료일을 수정했습니다.`);
    });
  };

  const handleDelete = async (item: ShortUrlItem) => {
    if (!window.confirm(`'${item.shortKey}' 단축 URL 을 삭제하시겠습니까?`)) return;
    await runWithStatus(`delete-${item.id}`, async () => {
      await apiRequest(`/api/short-url/${item.id}`, { method: "DELETE" });
      if (selected?.id === item.id) {
        setSelected(null);
        setEditForm({ expiredAt: "" });
      }
      setSuccess(`단축 URL(${item.shortKey}) 을 삭제했습니다.`);
      if (items.length === 1 && page > 0) setPage(page - 1);
      else await fetchList(page);
    });
  };

  const handleRedirectTest = (item: ShortUrlItem) => {
    window.open(item.shortUrl, "_blank", "noopener");
    setSuccess(`${item.shortKey} 리다이렉트 테스트 시작 — 잠시 후 이력 페이지를 확인하세요.`);
  };

  const handleCopy = (value: string) => {
    navigator.clipboard
      ?.writeText(value)
      .then(() => setSuccess("클립보드에 복사했습니다."))
      .catch(() => setError("복사에 실패했습니다."));
  };

  const disablePrev = page === 0;
  const disableNext = page >= totalPages - 1 || total === 0;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">단축 URL 생성</CardTitle>
          <CardDescription>원본 URL 과 만료 정책을 지정해 새 단축 키를 발급합니다.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">원본 URL *</p>
            <Input
              placeholder="https://example.com/..."
              value={createForm.longUrl}
              onChange={(e) => setCreateForm({ ...createForm, longUrl: e.target.value })}
            />
          </div>

          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">만료 정책</p>
            <div className="flex flex-wrap gap-3 text-sm">
              {(
                [
                  { value: "none", label: "기본(서버 설정)" },
                  { value: "validDays", label: "유효 기간(일)" },
                  { value: "expireDate", label: "만료 일시 지정" }
                ] as const
              ).map((opt) => (
                <label key={opt.value} className="flex items-center gap-1 cursor-pointer">
                  <input
                    type="radio"
                    name="expire-mode"
                    checked={createForm.mode === opt.value}
                    onChange={() => setCreateForm({ ...createForm, mode: opt.value })}
                  />
                  <span>{opt.label}</span>
                </label>
              ))}
            </div>
          </div>

          {createForm.mode === "validDays" && (
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">유효 기간(일) — 오늘 기준 N 일 뒤 23:59:59 로 설정</p>
              <Input
                type="number"
                min={1}
                value={createForm.validDays}
                onChange={(e) => setCreateForm({ ...createForm, validDays: e.target.value })}
              />
            </div>
          )}

          {createForm.mode === "expireDate" && (
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">만료 일시 (절대 시각)</p>
              <input
                type="datetime-local"
                className="flex h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-950 focus-visible:ring-offset-2 dark:border-slate-800 dark:bg-slate-950"
                value={createForm.expireDate}
                onChange={(e) => setCreateForm({ ...createForm, expireDate: e.target.value })}
              />
            </div>
          )}

          <div className="flex items-center gap-2">
            <Button className="flex-1" onClick={handleCreate} disabled={isBusy("create")}>
              {isBusy("create") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              단축 URL 생성
            </Button>
            <Button type="button" variant="outline" onClick={() => setCreateForm(INITIAL_CREATE_FORM)}>
              초기화
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Short Key 로 조회</CardTitle>
          <CardDescription>발급된 키를 입력해 상세 정보를 확인합니다 (GET /api/short-url/key/{`{key}`}).</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2">
            <Input
              placeholder="예: Ab3dEf"
              value={searchKey}
              onChange={(e) => setSearchKey(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearchByKey()}
            />
            <Button onClick={handleSearchByKey} disabled={isBusy("search-key")}>
              {isBusy("search-key") ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
              조회
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <CardTitle className="text-base">단축 URL 목록</CardTitle>
            <CardDescription>페이징 목록에서 생성일, 만료일, 상태를 확인하세요.</CardDescription>
          </div>
          <div className="flex gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => fetchList(page)} disabled={isFetching}>
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
                  <TableHead className="w-36">생성일</TableHead>
                  <TableHead className="w-36">만료일</TableHead>
                  <TableHead className="w-24 text-center">상태</TableHead>
                  <TableHead className="w-36 text-center">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isFetching ? (
                  <TableRow>
                    <TableCell colSpan={6} className="py-8 text-center text-sm text-slate-500">
                      데이터를 불러오는 중입니다...
                    </TableCell>
                  </TableRow>
                ) : items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="py-8 text-center text-sm text-slate-500">
                      등록된 단축 URL 이 없습니다.
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
                      <TableCell className="max-w-[320px] truncate text-xs text-slate-500">{item.longUrl}</TableCell>
                      <TableCell className="text-xs text-slate-500">{formatDateTime(item.createdAt)}</TableCell>
                      <TableCell className="text-xs text-slate-500">{formatDateTime(item.expiredAt)}</TableCell>
                      <TableCell className="text-center">
                        <Badge variant={isExpired(item.expiredAt) ? "outline" : "secondary"}>
                          {isExpired(item.expiredAt) ? "만료" : "활성"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="상세"
                            onClick={() => handleSelect(item)}
                            disabled={isBusy(`detail-${item.id}`)}
                          >
                            {isBusy(`detail-${item.id}`) ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="리다이렉트 테스트"
                            onClick={() => handleRedirectTest(item)}
                          >
                            <ExternalLink className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-rose-600 hover:text-rose-700"
                            aria-label="삭제"
                            onClick={() => handleDelete(item)}
                            disabled={isBusy(`delete-${item.id}`)}
                          >
                            {isBusy(`delete-${item.id}`) ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
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
              <Button type="button" variant="outline" size="sm" onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={disablePrev}>
                이전
              </Button>
              <Button type="button" variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={disableNext}>
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
                <Button variant="ghost" size="icon" onClick={() => handleCopy(selected.shortUrl)} aria-label="복사">
                  <Copy className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => window.open(selected.shortUrl, "_blank", "noopener")}
                  aria-label="새 탭"
                >
                  <LinkIcon className="h-4 w-4" />
                </Button>
              </div>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">원본 URL</p>
              <p className="text-sm break-all text-slate-700 dark:text-slate-200">{selected.longUrl}</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">만료일</p>
              <input
                type="datetime-local"
                className="flex h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-800 dark:bg-slate-950"
                value={editForm.expiredAt}
                onChange={(e) => setEditForm({ expiredAt: e.target.value })}
              />
              <p className="text-[10px] text-slate-500">서버 전송 형식: yyyy-MM-dd&apos;T&apos;HH:mm:ss</p>
            </div>
            <div className="flex flex-col gap-1">
              <p className="text-xs text-slate-500">생성일</p>
              <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{formatDateTime(selected.createdAt)}</p>
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
