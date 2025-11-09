import { useCallback, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { Loader2, Copy, Trash2, Search, RefreshCw, XCircle } from "lucide-react";

const formatDateTime = (value?: string | null) => (value ? new Date(value).toLocaleString() : "-");

type ApiEnvelope<T> = {
  code: string;
  message: string;
  data: T;
};

type ServerAuthKeyInfo = {
  id: number;
  name: string;
  keyValue: string;
  issuedBy?: string;
  description?: string;
  expiresAt?: string;
  lastUsedAt?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export function ServerAuthKeyPage() {
  const [items, setItems] = useState<ServerAuthKeyInfo[]>([]);
  const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [createForm, setCreateForm] = useState({ name: "", issuedBy: "", description: "", expiresAt: "" });
  const [editing, setEditing] = useState<ServerAuthKeyInfo | null>(null);
  const [editForm, setEditForm] = useState({ name: "", description: "", expiresAt: "", active: true });

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

  const runWithStatus = async (label: string, fn: () => Promise<void>) => {
    setBusyAction(label);
    setStatus(null);
    try {
      await fn();
    } catch (error) {
      const message = error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.";
      setStatus({ type: "error", message });
    } finally {
      setBusyAction(null);
    }
  };

  const handleFetch = async () => {
    await runWithStatus("fetch", async () => {
      const list = await request<ServerAuthKeyInfo[]>("/api/server-keys");
      setItems(list);
      setStatus({ type: "success", message: `총 ${list.length}개의 서버 인증 키를 불러왔습니다.` });
    });
  };

  const handleCreate = async () => {
    if (!createForm.name.trim()) {
      setStatus({ type: "error", message: "이름을 입력해주세요." });
      return;
    }

    await runWithStatus("create", async () => {
      const payload = await request<ServerAuthKeyInfo>("/api/server-keys", {
        method: "POST",
        body: JSON.stringify({
          ...createForm,
          expiresAt: createForm.expiresAt ? new Date(createForm.expiresAt).toISOString() : null
        })
      });
      setItems((prev) => [payload, ...prev]);
      setCreateForm({ name: "", issuedBy: "", description: "", expiresAt: "" });
      setStatus({ type: "success", message: `'${payload.name}' 키가 발급되었습니다.` });
    });
  };

  const handleEdit = (item: ServerAuthKeyInfo) => {
    setEditing(item);
    setEditForm({
      name: item.name,
      description: item.description ?? "",
      expiresAt: item.expiresAt ? item.expiresAt.slice(0, 16) : "",
      active: item.active
    });
  };

  const handleUpdate = async () => {
    if (!editing) return;
    await runWithStatus(`update-${editing.id}`, async () => {
      const payload = await request<ServerAuthKeyInfo>(`/api/server-keys/${editing.id}`, {
        method: "PUT",
        body: JSON.stringify({
          name: editForm.name,
          description: editForm.description,
          active: editForm.active,
          expiresAt: editForm.expiresAt ? new Date(editForm.expiresAt).toISOString() : null
        })
      });
      setItems((prev) => prev.map((item) => (item.id === payload.id ? payload : item)));
      setEditing(null);
      setStatus({ type: "success", message: `'${payload.name}' 키를 수정했습니다.` });
    });
  };

  const handleDelete = async (item: ServerAuthKeyInfo) => {
    if (!confirm(`'${item.name}' 키를 삭제하시겠습니까?`)) return;
    await runWithStatus(`delete-${item.id}`, async () => {
      await request(`/api/server-keys/${item.id}`, { method: "DELETE" });
      setItems((prev) => prev.filter((entry) => entry.id !== item.id));
      if (editing?.id === item.id) {
        setEditing(null);
      }
      setStatus({ type: "success", message: `'${item.name}' 키를 삭제했습니다.` });
    });
  };

  const copyValue = (value: string) => {
    navigator.clipboard?.writeText(value).then(() => setStatus({ type: "success", message: "복사되었습니다." }));
  };

  const isBusy = (label: string) => busyAction === label;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">서버 인증 키 관리</CardTitle>
          <CardDescription>서비스 별로 사용할 서버 인증 키를 발급합니다.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2">
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">이름 *</p>
            <Input
              value={createForm.name}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, name: event.target.value }))}
              placeholder="예: NH-Bank"
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">발급자</p>
            <Input
              value={createForm.issuedBy}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, issuedBy: event.target.value }))}
              placeholder="infra-team"
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">만료일 (선택)</p>
            <Input
              type="datetime-local"
              value={createForm.expiresAt}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, expiresAt: event.target.value }))}
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">설명</p>
            <Input
              value={createForm.description}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, description: event.target.value }))}
              placeholder="비고"
            />
          </div>
          <div className="md:col-span-2 flex flex-wrap gap-2">
            <Button type="button" onClick={handleCreate} disabled={isBusy("create")}>
              {isBusy("create") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              서버 인증 키 발급
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => setCreateForm({ name: "", issuedBy: "", description: "", expiresAt: "" })}
            >
              초기화
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <CardTitle className="text-base">발급 목록</CardTitle>
            <CardDescription>활성 상태, 만료일, 사용 이력을 확인합니다.</CardDescription>
          </div>
          <Button type="button" variant="outline" size="sm" onClick={handleFetch} disabled={isBusy("fetch")}>
            {isBusy("fetch") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            목록 새로고침
          </Button>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>No</TableHead>
                  <TableHead>이름</TableHead>
                  <TableHead>키 값</TableHead>
                  <TableHead>발급자</TableHead>
                  <TableHead>만료일</TableHead>
                  <TableHead>상태</TableHead>
                  <TableHead className="w-28 text-center">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-6 text-center text-sm text-slate-500">
                      등록된 서버 인증 키가 없습니다. 상단의 발급 폼을 사용해 생성하세요.
                    </TableCell>
                  </TableRow>
                ) : (
                  items.map((item, index) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-mono text-xs text-slate-500">{items.length - index}</TableCell>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs">{item.keyValue}</span>
                          <Button variant="ghost" size="icon" onClick={() => copyValue(item.keyValue)}>
                            <Copy className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                      <TableCell>{item.issuedBy || "-"}</TableCell>
                      <TableCell>{formatDateTime(item.expiresAt)}</TableCell>
                      <TableCell>
                        <Badge variant={item.active ? "secondary" : "outline"}>
                          {item.active ? "활성" : "비활성"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center gap-2">
                          <Button variant="ghost" size="icon" aria-label="수정" onClick={() => handleEdit(item)}>
                            <Search className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-rose-600 hover:text-rose-700"
                            aria-label="삭제"
                            onClick={() => handleDelete(item)}
                            disabled={isBusy(`delete-${item.id}`)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {editing && (
        <Card>
          <CardHeader className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
            <div>
              <CardTitle className="text-base">서버 인증 키 수정</CardTitle>
              <CardDescription>{editing.name} 설정을 변경합니다.</CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setEditing(null)}>
                취소
              </Button>
              <Button onClick={handleUpdate} disabled={isBusy(`update-${editing.id}`)}>
                {isBusy(`update-${editing.id}`) && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                저장
              </Button>
            </div>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">이름</p>
              <Input value={editForm.name} onChange={(event) => setEditForm((prev) => ({ ...prev, name: event.target.value }))} />
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">만료일</p>
              <Input
                type="datetime-local"
                value={editForm.expiresAt}
                onChange={(event) => setEditForm((prev) => ({ ...prev, expiresAt: event.target.value }))}
              />
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">설명</p>
              <Input
                value={editForm.description}
                onChange={(event) => setEditForm((prev) => ({ ...prev, description: event.target.value }))}
              />
            </div>
            <div className="flex items-center gap-2 pt-4">
              <input
                id="server-key-active"
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300"
                checked={editForm.active}
                onChange={(event) => setEditForm((prev) => ({ ...prev, active: event.target.checked }))}
              />
              <label htmlFor="server-key-active" className="text-sm text-slate-600">
                활성 상태
              </label>
            </div>
          </CardContent>
        </Card>
      )}

      {status && (
        <div
          className={`flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${
            status.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-800"
          }`}
        >
          {status.type === "success" ? <RefreshCw className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
          <span>{status.message}</span>
        </div>
      )}
    </div>
  );
}
