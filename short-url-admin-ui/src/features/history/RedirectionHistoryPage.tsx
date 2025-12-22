import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, RefreshCw, Search, Clock, Globe, Laptop, HardDrive, BarChart3, ListFilter, AlertCircle } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Input } from "../../components/ui/input";
import { Checkbox } from "../../components/ui/checkbox";
import { cn } from "../../lib/utils";

type ApiEnvelope<T> = {
    code: string;
    message: string;
    data: T;
};

type RedirectionHistoryItem = {
    id: number;
    shortUrlId: number;
    shortKey: string;
    referer: string;
    userAgent: string;
    ip: string;
    deviceType: string;
    os: string;
    browser: string;
    country: string;
    city: string;
    redirectAt: string;
    botType: "CALLBOT" | "CHATBOT" | null;
    botServiceKey: string | null;
    surveyId: string | null;
    surveyVer: string | null;
};

type PageResponse<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
};

type StatRow = {
    [key: string]: any;
    count: number;
};

const PAGE_SIZE = 20;

const formatDateTime = (value?: string | null) => (value ? new Date(value).toLocaleString() : "-");

const GROUP_BY_OPTIONS = [
    { value: "REFERER", label: "레퍼러" },
    { value: "DEVICE_TYPE", label: "디바이스" },
    { value: "OS", label: "OS" },
    { value: "BROWSER", label: "브라우저" },
    { value: "COUNTRY", label: "국가" },
    { value: "CITY", label: "도시" },
    { value: "YEAR", label: "연도" },
    { value: "MONTH", label: "월" },
    { value: "DAY", label: "일" },
];

const REG_STORAGE_KEY = "shorturl:accessKey";

const DUMMY_SURVEYS = [
    { id: "S001", name: "농협 고객 만족도 통합 조사", version: "V1.2" },
    { id: "S002", name: "대출 상담 프로세스 만족도 조사", version: "V2.0" },
    { id: "S003", name: "전자금융 서비스 이용 만족도 조사", version: "V1.0" },
    { id: "S004", name: "영업점 친절도 정기 조사", version: "V3.1" },
];

export function RedirectionHistoryPage() {
    const [accessKey, setAccessKey] = useState(() => {
        if (typeof window === "undefined") return "";
        return localStorage.getItem(REG_STORAGE_KEY) ?? "";
    });
    const [items, setItems] = useState<RedirectionHistoryItem[]>([]);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [selected, setSelected] = useState<RedirectionHistoryItem | null>(null);
    const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
    const [isFetching, setIsFetching] = useState(false);

    // Stats States
    const [searchKey, setSearchKey] = useState("");
    const [targetUrl, setTargetUrl] = useState<{ id: number; shortKey: string } | null>(null);
    const [selectedGroups, setSelectedGroups] = useState<string[]>(["COUNTRY", "BROWSER"]);
    const [stats, setStats] = useState<StatRow[]>([]);
    const [isFetchingStats, setIsFetchingStats] = useState(false);

    const totalPages = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);

    const request = useCallback(
        async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
            const headers = new Headers(init.headers);
            if (accessKey.trim()) {
                headers.set("X-CLIENTACCESS-KEY", accessKey.trim());
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
        [accessKey]
    );

    const fetchList = useCallback(
        async (pageToLoad: number) => {
            setIsFetching(true);
            try {
                const data = await request<PageResponse<RedirectionHistoryItem>>(`/api/redirections/history?page=${pageToLoad}&size=${PAGE_SIZE}&sort=redirectAt,desc`);
                setItems(data?.content ?? []);
                setTotal(data?.totalElements ?? 0);
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

    const handleRefresh = () => {
        setStatus(null);
        fetchList(page);
    };

    const handleSelectHistory = async (id: number) => {
        try {
            const detail = await request<RedirectionHistoryItem>(`/api/redirections/history/${id}`);
            setSelected(detail);
        } catch (error) {
            const message = error instanceof Error ? error.message : "상세 정보를 불러오는 중 오류가 발생했습니다.";
            setStatus({ type: "error", message });
        }
    };

    const handleSearchUrl = async () => {
        if (!searchKey.trim()) return;
        setIsFetchingStats(true);
        try {
            // 키로 URL 정보 조회
            const response = await request<any>(`/api/short-url/key/${searchKey.trim()}`);
            if (response) {
                setTargetUrl({ id: response.id, shortKey: response.shortKey });
                // 통계 자동 조회
                fetchStats(response.id, selectedGroups);
            }
        } catch (error) {
            setStatus({ type: "error", message: "일치하는 단축 URL을 찾을 수 없습니다." });
            setTargetUrl(null);
            setStats([]);
        } finally {
            setIsFetchingStats(false);
        }
    };

    const fetchStats = async (id: number, groups: string[]) => {
        setIsFetchingStats(true);
        try {
            const data = await request<StatRow[]>(`/api/redirections/history/${id}/stats`, {
                method: "POST",
                body: JSON.stringify({ groupBy: groups })
            });
            setStats(data || []);
        } catch (error) {
            setStatus({ type: "error", message: "통계 정보를 불러오는 중 오류가 발생했습니다." });
        } finally {
            setIsFetchingStats(false);
        }
    };

    const toggleGroup = (value: string) => {
        setSelectedGroups(prev =>
            prev.includes(value) ? prev.filter(v => v !== value) : [...prev, value]
        );
    };

    const disablePrev = page === 0;
    const disableNext = page >= totalPages - 1 || total === 0;

    return (
        <div className="flex flex-col gap-6">
            <div className="flex flex-col gap-2">
                <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">리디렉션 이력</h1>
                <p className="text-slate-500 dark:text-slate-400">단축 URL의 유입 기록과 통계 데이터를 확인하고 분석합니다.</p>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle className="text-sm">인증 설정</CardTitle>
                </CardHeader>
                <CardContent className="flex gap-2">
                    <Input
                        value={accessKey}
                        onChange={(e) => {
                            const val = e.target.value;
                            setAccessKey(val);
                            localStorage.setItem(REG_STORAGE_KEY, val);
                        }}
                        placeholder="Client Access Key"
                        className="max-w-md"
                    />
                    {accessKey && (
                        <Button variant="outline" onClick={() => {
                            setAccessKey("");
                            localStorage.removeItem(REG_STORAGE_KEY);
                        }}>초기화</Button>
                    )}
                </CardContent>
            </Card>

            <Tabs defaultValue="history" className="w-full">
                <TabsList className="grid w-full grid-cols-2 mb-4">
                    <TabsTrigger value="history" className="flex items-center gap-2">
                        <Clock className="h-4 w-4" />
                        최근 리디렉션 이력
                    </TabsTrigger>
                    <TabsTrigger value="stats" className="flex items-center gap-2">
                        <BarChart3 className="h-4 w-4" />
                        단축 URL별 통계 분석
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="history" className="space-y-4">
                    <Card>
                        <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                            <div>
                                <CardTitle className="text-base font-semibold">전체 접속 로그</CardTitle>
                                <CardDescription>가장 최근에 발생한 리디렉션부터 순서대로 표시됩니다.</CardDescription>
                            </div>
                            <Button type="button" variant="outline" size="sm" onClick={handleRefresh} disabled={isFetching}>
                                {isFetching ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                                목록 새로고침
                            </Button>
                        </CardHeader>
                        <CardContent>
                            <div className="overflow-x-auto rounded-md border">
                                <Table>
                                    <TableHeader>
                                        <TableRow className="bg-slate-50 dark:bg-slate-900/50">
                                            <TableHead className="w-16 text-center">ID</TableHead>
                                            <TableHead className="w-32">Short Key</TableHead>
                                            <TableHead className="w-48 text-center">접속 일시</TableHead>
                                            <TableHead className="w-32 text-center">봇 구분</TableHead>
                                            <TableHead className="w-40 text-center">참조 키</TableHead>
                                            <TableHead className="w-24 text-center">디바이스</TableHead>
                                            <TableHead className="w-36 text-center">OS / 브라우저</TableHead>
                                            <TableHead className="w-16 text-center">작업</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {isFetching ? (
                                            <TableRow>
                                                <TableCell colSpan={8} className="py-12 text-center">
                                                    <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
                                                </TableCell>
                                            </TableRow>
                                        ) : items.length === 0 ? (
                                            <TableRow>
                                                <TableCell colSpan={8} className="py-12 text-center text-slate-500">이력이 없습니다.</TableCell>
                                            </TableRow>
                                        ) : (
                                            items.map((item) => (
                                                <TableRow key={item.id} className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-900/40" onClick={() => handleSelectHistory(item.id)}>
                                                    <TableCell className="text-center font-mono text-[11px] text-slate-400">{item.id}</TableCell>
                                                    <TableCell className="font-semibold text-blue-600 dark:text-blue-400">{item.shortKey}</TableCell>
                                                    <TableCell className="text-center text-xs">{formatDateTime(item.redirectAt)}</TableCell>
                                                    <TableCell className="text-center">
                                                        {item.botType ? (
                                                            <Badge variant="outline" className="text-[10px]">{item.botType === "CALLBOT" ? "콜봇" : "챗봇"}</Badge>
                                                        ) : (
                                                            <span className="text-slate-400 text-[10px]">-</span>
                                                        )}
                                                    </TableCell>
                                                    <TableCell className="text-center font-mono text-[11px] truncate max-w-[120px]">{item.botServiceKey || "-"}</TableCell>
                                                    <TableCell className="text-center">
                                                        <Badge variant="secondary" className="text-[10px] font-normal">{item.deviceType || "?"}</Badge>
                                                    </TableCell>
                                                    <TableCell className="text-center">
                                                        <div className="flex flex-col text-[10px]">
                                                            <span>{item.os}</span>
                                                            <span className="text-slate-400">{item.browser}</span>
                                                        </div>
                                                    </TableCell>
                                                    <TableCell className="text-center">
                                                        <Button variant="ghost" size="icon" className="h-7 w-7">
                                                            <Search className="h-3.5 w-3.5" />
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))
                                        )}
                                    </TableBody>
                                </Table>
                            </div>
                            <div className="flex items-center justify-between mt-4">
                                <span className="text-xs text-slate-500">총 {total.toLocaleString()}건</span>
                                <div className="flex gap-1">
                                    <Button variant="outline" size="sm" onClick={() => setPage(p => p - 1)} disabled={disablePrev}>이전</Button>
                                    <Button variant="outline" size="sm" onClick={() => setPage(p => p + 1)} disabled={disableNext}>다음</Button>
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    {selected && (
                        <Card className="bg-slate-900 text-slate-100 dark:bg-black">
                            <CardHeader className="border-b border-slate-800 flex flex-row items-center justify-between">
                                <div>
                                    <CardTitle className="text-sm">세부 로그 분석</CardTitle>
                                </div>
                                <Button variant="ghost" size="sm" onClick={() => setSelected(null)} className="text-slate-400 hover:text-white">닫기</Button>
                            </CardHeader>
                            <CardContent className="py-4">
                                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">Referer</p>
                                        <p className="text-xs break-all">{selected.referer || "Direct"}</p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">Client Info</p>
                                        <p className="text-xs">{selected.ip} ({selected.city}, {selected.country})</p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">봇 구분</p>
                                        <p className="text-xs">
                                            {selected.botType ? (selected.botType === "CALLBOT" ? "콜봇" : "챗봇") : "-"}
                                        </p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">참조 키</p>
                                        <p className="text-xs font-mono">{selected.botServiceKey || "-"}</p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">설문 정보</p>
                                        <p className="text-xs">
                                            {selected.surveyId ? (DUMMY_SURVEYS.find(s => s.id === selected.surveyId)?.name || selected.surveyId) : "-"}
                                            {selected.surveyVer ? ` (${selected.surveyVer})` : ""}
                                        </p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">Redirect At</p>
                                        <p className="text-xs">{formatDateTime(selected.redirectAt)}</p>
                                    </div>
                                    <div className="space-y-1 lg:col-span-4">
                                        <p className="text-[10px] uppercase text-slate-500 font-bold">User Agent</p>
                                        <p className="text-[10px] font-mono break-all text-slate-400">{selected.userAgent}</p>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    )}
                </TabsContent>

                <TabsContent value="stats" className="space-y-4">
                    <div className="grid gap-6 md:grid-cols-[300px_1fr]">
                        <Card className="h-fit">
                            <CardHeader>
                                <CardTitle className="text-sm">조회 조건 설정</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-6">
                                <div className="space-y-2">
                                    <label className="text-xs font-semibold text-slate-500">Short Key 검색</label>
                                    <div className="flex gap-2">
                                        <Input
                                            placeholder="예: nh82k"
                                            value={searchKey}
                                            onChange={e => setSearchKey(e.target.value)}
                                            onKeyDown={e => e.key === 'Enter' && handleSearchUrl()}
                                        />
                                        <Button size="icon" onClick={handleSearchUrl} disabled={isFetchingStats}>
                                            <Search className="h-4 w-4" />
                                        </Button>
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <label className="text-xs font-semibold text-slate-500">그룹 바이 필드 (다중 선택)</label>
                                    <div className="grid grid-cols-2 gap-2">
                                        {GROUP_BY_OPTIONS.map(opt => (
                                            <div key={opt.value} className="flex items-center space-x-2">
                                                <Checkbox
                                                    id={`grp-${opt.value}`}
                                                    checked={selectedGroups.includes(opt.value)}
                                                    onCheckedChange={() => toggleGroup(opt.value)}
                                                />
                                                <label htmlFor={`grp-${opt.value}`} className="text-xs cursor-pointer">{opt.label}</label>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <Button
                                    className="w-full"
                                    disabled={!targetUrl || isFetchingStats}
                                    onClick={() => targetUrl && fetchStats(targetUrl.id, selectedGroups)}
                                >
                                    통계 새로고침
                                </Button>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader>
                                <CardTitle className="text-base">
                                    {targetUrl ? `${targetUrl.shortKey} 통계 데이터` : "URL을 검색해주세요"}
                                </CardTitle>
                                <CardDescription>설정한 그룹별 필드 조합에 따른 접속 횟수 통계입니다.</CardDescription>
                            </CardHeader>
                            <CardContent>
                                {isFetchingStats ? (
                                    <div className="py-20 text-center">
                                        <Loader2 className="mx-auto h-8 w-8 animate-spin text-blue-500" />
                                    </div>
                                ) : !targetUrl ? (
                                    <div className="py-20 text-center flex flex-col items-center gap-2 text-slate-400">
                                        <ListFilter className="h-10 w-10 opacity-20" />
                                        <p className="text-sm">왼쪽 검색창에서 짧은 키를 입력하여 분석을 시작하세요.</p>
                                    </div>
                                ) : stats.length === 0 ? (
                                    <div className="py-20 text-center text-slate-500">데이터가 없습니다.</div>
                                ) : (
                                    <div className="overflow-x-auto rounded-md border">
                                        <Table>
                                            <TableHeader>
                                                <TableRow className="bg-slate-50">
                                                    {selectedGroups.map(g => (
                                                        <TableHead key={g}>{GROUP_BY_OPTIONS.find(o => o.value === g)?.label}</TableHead>
                                                    ))}
                                                    <TableHead className="w-24 text-right">횟수</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {stats.map((row, idx) => (
                                                    <TableRow key={idx}>
                                                        {selectedGroups.map(g => (
                                                            <TableCell key={g} className="text-xs">{row[g.toLowerCase()] || "-"}</TableCell>
                                                        ))}
                                                        <TableCell className="text-right font-bold text-blue-600">{row.count.toLocaleString()}</TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    </div>
                </TabsContent>
            </Tabs>

            {status && (
                <div className={cn(
                    "fixed bottom-4 right-4 flex items-center gap-3 rounded-lg border px-4 py-3 text-sm shadow-xl transition-all animate-in slide-in-from-right-4",
                    status.type === "success" ? "bg-emerald-50 border-emerald-200 text-emerald-800" : "bg-rose-50 border-rose-200 text-rose-800"
                )}>
                    <AlertCircle className="h-4 w-4" />
                    <span>{status.message}</span>
                    <button onClick={() => setStatus(null)} className="ml-2 hover:opacity-70">×</button>
                </div>
            )}
        </div>
    );
}
