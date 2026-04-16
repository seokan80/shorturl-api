import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, RefreshCw, Search, AlertCircle } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { cn } from "../../lib/utils";
import { apiRequest } from "../../lib/apiClient";
import { formatDateTime } from "../../lib/formats";

type RedirectionHistoryItem = {
    id: number;
    shortUrlId: number;
    shortKey: string;
    referer: string | null;
    userAgent: string | null;
    ip: string | null;
    deviceType: string | null;
    os: string | null;
    browser: string | null;
    country: string | null;
    city: string | null;
    redirectAt: string;
};

// Spring Page<T> 응답 형태
type PageResponse<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
};

const PAGE_SIZE = 20;

export function RedirectionHistoryPage() {
    const [items, setItems] = useState<RedirectionHistoryItem[]>([]);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [selected, setSelected] = useState<RedirectionHistoryItem | null>(null);
    const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
    const [isFetching, setIsFetching] = useState(false);
    const [isFetchingDetail, setIsFetchingDetail] = useState<number | null>(null);

    const disablePrev = page === 0;
    const disableNext = useMemo(() => page >= totalPages - 1 || total === 0, [page, totalPages, total]);

    const fetchList = useCallback(async (pageToLoad: number) => {
        setIsFetching(true);
        try {
            const data = await apiRequest<PageResponse<RedirectionHistoryItem>>(
                `/api/redirections/history?page=${pageToLoad}&size=${PAGE_SIZE}&sort=redirectAt,desc`
            );
            setItems(data?.content ?? []);
            setTotal(data?.totalElements ?? 0);
            setTotalPages(data?.totalPages ?? 1);
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

    const handleSelectHistory = async (id: number) => {
        setIsFetchingDetail(id);
        try {
            const detail = await apiRequest<RedirectionHistoryItem>(`/api/redirections/history/${id}`);
            setSelected(detail);
            setStatus(null);
        } catch (error) {
            const message = error instanceof Error ? error.message : "상세 정보를 불러오는 중 오류가 발생했습니다.";
            setStatus({ type: "error", message });
        } finally {
            setIsFetchingDetail(null);
        }
    };

    return (
        <div className="flex flex-col gap-6">
            <Card>
                <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div>
                        <CardTitle className="text-base font-semibold">전체 접속 로그</CardTitle>
                        <CardDescription>가장 최근에 발생한 리다이렉션부터 순서대로 표시됩니다.</CardDescription>
                    </div>
                    <Button type="button" variant="outline" size="sm" onClick={() => fetchList(page)} disabled={isFetching}>
                        {isFetching ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                        <span className="ml-1">새로고침</span>
                    </Button>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="overflow-x-auto rounded-md border">
                        <Table>
                            <TableHeader>
                                <TableRow className="bg-slate-50 dark:bg-slate-900/50">
                                    <TableHead className="w-16 text-center">ID</TableHead>
                                    <TableHead className="w-32">Short Key</TableHead>
                                    <TableHead className="w-44 text-center">접속 일시</TableHead>
                                    <TableHead className="w-32 text-center">IP</TableHead>
                                    <TableHead className="w-24 text-center">디바이스</TableHead>
                                    <TableHead className="w-36 text-center">OS / 브라우저</TableHead>
                                    <TableHead className="w-14 text-center">상세</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {isFetching ? (
                                    <TableRow>
                                        <TableCell colSpan={7} className="py-12 text-center">
                                            <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
                                        </TableCell>
                                    </TableRow>
                                ) : items.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={7} className="py-12 text-center text-slate-500">
                                            이력이 없습니다.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    items.map((item) => (
                                        <TableRow
                                            key={item.id}
                                            className={cn(
                                                "cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-900/40",
                                                selected?.id === item.id && "bg-slate-50 dark:bg-slate-900/30"
                                            )}
                                            onClick={() => handleSelectHistory(item.id)}
                                        >
                                            <TableCell className="text-center font-mono text-[11px] text-slate-400">{item.id}</TableCell>
                                            <TableCell className="font-semibold text-blue-600 dark:text-blue-400">{item.shortKey}</TableCell>
                                            <TableCell className="text-center text-xs">{formatDateTime(item.redirectAt)}</TableCell>
                                            <TableCell className="text-center font-mono text-[11px]">{item.ip ?? "-"}</TableCell>
                                            <TableCell className="text-center">
                                                <Badge variant="secondary" className="text-[10px] font-normal">
                                                    {item.deviceType ?? "?"}
                                                </Badge>
                                            </TableCell>
                                            <TableCell className="text-center">
                                                <div className="flex flex-col text-[10px]">
                                                    <span>{item.os ?? "-"}</span>
                                                    <span className="text-slate-400">{item.browser ?? "-"}</span>
                                                </div>
                                            </TableCell>
                                            <TableCell className="text-center">
                                                {isFetchingDetail === item.id ? (
                                                    <Loader2 className="mx-auto h-3.5 w-3.5 animate-spin" />
                                                ) : (
                                                    <Button variant="ghost" size="icon" className="h-7 w-7">
                                                        <Search className="h-3.5 w-3.5" />
                                                    </Button>
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-500">
                            총 {total.toLocaleString()}건 · 페이지 {total === 0 ? 0 : page + 1}/{totalPages}
                        </span>
                        <div className="flex gap-1">
                            <Button variant="outline" size="sm" onClick={() => setPage((p) => p - 1)} disabled={disablePrev}>이전</Button>
                            <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={disableNext}>다음</Button>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {selected && (
                <Card className="bg-slate-900 text-slate-100 dark:bg-black">
                    <CardHeader className="border-b border-slate-800 flex flex-row items-center justify-between">
                        <CardTitle className="text-sm">세부 로그 · #{selected.id} ({selected.shortKey})</CardTitle>
                        <Button variant="ghost" size="sm" onClick={() => setSelected(null)} className="text-slate-400 hover:text-white">닫기</Button>
                    </CardHeader>
                    <CardContent className="py-4">
                        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                            <div className="space-y-1">
                                <p className="text-[10px] uppercase text-slate-500 font-bold tracking-wide">Referer</p>
                                <p className="text-xs break-all">{selected.referer ?? "Direct"}</p>
                            </div>
                            <div className="space-y-1">
                                <p className="text-[10px] uppercase text-slate-500 font-bold tracking-wide">Client</p>
                                <p className="text-xs">{selected.ip}
                                    {(selected.city || selected.country) && (
                                        <span className="text-slate-400"> ({[selected.city, selected.country].filter(Boolean).join(", ")})</span>
                                    )}
                                </p>
                            </div>
                            <div className="space-y-1">
                                <p className="text-[10px] uppercase text-slate-500 font-bold tracking-wide">Redirect At</p>
                                <p className="text-xs">{formatDateTime(selected.redirectAt)}</p>
                            </div>
                            <div className="space-y-1">
                                <p className="text-[10px] uppercase text-slate-500 font-bold tracking-wide">Device</p>
                                <p className="text-xs">{[selected.deviceType, selected.os, selected.browser].filter(Boolean).join(" / ")}</p>
                            </div>
                            <div className="space-y-1 lg:col-span-4">
                                <p className="text-[10px] uppercase text-slate-500 font-bold tracking-wide">User Agent</p>
                                <p className="text-[10px] font-mono break-all text-slate-400">{selected.userAgent ?? "-"}</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            )}

            {status && (
                <div className={cn(
                    "fixed bottom-4 right-4 flex items-center gap-3 rounded-lg border px-4 py-3 text-sm shadow-xl",
                    status.type === "success"
                        ? "bg-emerald-50 border-emerald-200 text-emerald-800"
                        : "bg-rose-50 border-rose-200 text-rose-800"
                )}>
                    <AlertCircle className="h-4 w-4" />
                    <span>{status.message}</span>
                    <button onClick={() => setStatus(null)} className="ml-2 hover:opacity-70">×</button>
                </div>
            )}
        </div>
    );
}
