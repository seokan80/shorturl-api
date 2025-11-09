import { useCallback, useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { Loader2, RefreshCw, Trash2, Copy, Shield, Users, XCircle, Search } from "lucide-react";

type UserSummary = {
  id: number;
  username: string;
  createdAt?: string;
  updatedAt?: string;
};

type UserDetail = {
  id: number;
  username: string;
  createdAt?: string;
  updatedAt?: string;
};

type TokenPayload = {
  token: string;
  refreshToken: string;
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

type ServerAuthKeyForm = {
  name: string;
  issuedBy: string;
  description: string;
  expiresAt: string;
};

type ServerAuthKeyEditForm = {
  name: string;
  description: string;
  expiresAt: string;
  active: boolean;
};

type ApiEnvelope<T> = {
  code: string;
  message: string;
  data: T;
};

const REG_STORAGE_KEY = "shorturl:registrationKey";
const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString() : "-");

export function UserManagementPage() {
  const [registrationKey, setRegistrationKey] = useState(() => {
    if (typeof window === "undefined") return "";
    return localStorage.getItem(REG_STORAGE_KEY) ?? "";
  });
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [selectedUser, setSelectedUser] = useState<UserDetail | null>(null);
  const [tokenCache, setTokenCache] = useState<
    Record<string, TokenPayload & { username: string; issuedAt: string }>
  >({});
  const [serverKeys, setServerKeys] = useState<ServerAuthKeyInfo[]>([]);
  const [serverKeyForm, setServerKeyForm] = useState<ServerAuthKeyForm>({
    name: "",
    issuedBy: "",
    description: "",
    expiresAt: ""
  });
  const [editingServerKey, setEditingServerKey] = useState<ServerAuthKeyInfo | null>(null);
  const [serverKeyEditForm, setServerKeyEditForm] = useState<ServerAuthKeyEditForm>({
    name: "",
    description: "",
    expiresAt: "",
    active: true
  });
  const [newUsername, setNewUsername] = useState("");
  const [status, setStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (registrationKey) {
      localStorage.setItem(REG_STORAGE_KEY, registrationKey);
    } else {
      localStorage.removeItem(REG_STORAGE_KEY);
    }
  }, [registrationKey]);

  const setSuccess = (message: string) => setStatus({ type: "success", message });
  const setError = (message: string) => setStatus({ type: "error", message });

  const request = useCallback(
    async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
      const key = registrationKey.trim();
      if (!key) {
        throw new Error("먼저 서버 인증 키를 입력해주세요.");
      }

      const headers = new Headers(init.headers);
      headers.set("X-REGISTRATION-KEY", key);
      if (init.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
      }

      const response = await fetch(path, { ...init, headers });

      const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;

      if (!payload) {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status} 오류가 발생했습니다.`);
        }
        return undefined as T;
      }

      if (!response.ok || payload.code !== "0000") {
        throw new Error(payload.message ?? "요청에 실패했습니다.");
      }

      return payload.data;
    },
    [registrationKey]
  );

  const runWithStatus = async (label: string, fn: () => Promise<void>) => {
    setBusyAction(label);
    setStatus(null);
    try {
      await fn();
    } catch (error) {
      const message = error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.";
      setError(message);
    } finally {
      setBusyAction(null);
    }
  };

  const handleFetchUsers = async () => {
    await runWithStatus("fetchUsers", async () => {
      const list = await request<UserSummary[]>("/api/auth/users");
      setUsers(list);
      setSuccess(`총 ${list.length}명의 사용자를 불러왔습니다.`);
    });
  };

  const handleFetchServerKeys = async () => {
    await runWithStatus("fetchServerKeys", async () => {
      const list = await request<ServerAuthKeyInfo[]>("/api/server-keys");
      setServerKeys(list);
      setSuccess(`서버 인증 키 ${list.length}건을 불러왔습니다.`);
    });
  };

  const handleCreateUser = async () => {
    const username = newUsername.trim();
    if (!username) {
      setError("사용자명을 입력해주세요.");
      return;
    }
    await runWithStatus("createUser", async () => {
      await request("/api/auth/users", {
        method: "POST",
        body: JSON.stringify({ username })
      });
      setNewUsername("");
      setSuccess(`${username} 사용자가 등록되었습니다.`);
      await handleFetchUsers();
    });
  };

  const handleCreateServerKey = async () => {
    if (!serverKeyForm.name.trim()) {
      setError("서버 인증 키 이름을 입력해주세요.");
      return;
    }
    await runWithStatus("createServerKey", async () => {
      const payload = await request<ServerAuthKeyInfo>("/api/server-keys", {
        method: "POST",
        body: JSON.stringify({
          ...serverKeyForm,
          expiresAt: serverKeyForm.expiresAt ? new Date(serverKeyForm.expiresAt).toISOString() : null
        })
      });
      setServerKeyForm({ name: "", issuedBy: "", description: "", expiresAt: "" });
      setServerKeys((prev) => [payload, ...prev]);
      setSuccess(`서버 인증 키 '${payload.name}'이 발급되었습니다.`);
    });
  };

  const handleEditServerKey = (key: ServerAuthKeyInfo) => {
    setEditingServerKey(key);
    setServerKeyEditForm({
      name: key.name,
      description: key.description ?? "",
      expiresAt: key.expiresAt ? key.expiresAt.slice(0, 16) : "",
      active: key.active
    });
  };

  const handleUpdateServerKey = async () => {
    if (!editingServerKey) return;
    await runWithStatus(`updateServerKey-${editingServerKey.id}`, async () => {
      const payload = await request<ServerAuthKeyInfo>(`/api/server-keys/${editingServerKey.id}`, {
        method: "PUT",
        body: JSON.stringify({
          name: serverKeyEditForm.name,
          description: serverKeyEditForm.description,
          active: serverKeyEditForm.active,
          expiresAt: serverKeyEditForm.expiresAt ? new Date(serverKeyEditForm.expiresAt).toISOString() : null
        })
      });
      setServerKeys((prev) => prev.map((item) => (item.id === payload.id ? payload : item)));
      setEditingServerKey(null);
      setSuccess(`'${payload.name}' 서버 인증 키를 수정했습니다.`);
    });
  };

  const handleDeleteServerKey = async (key: ServerAuthKeyInfo) => {
    if (!confirm(`'${key.name}' 키를 삭제하시겠습니까?`)) return;
    await runWithStatus(`deleteServerKey-${key.id}`, async () => {
      await request(`/api/server-keys/${key.id}`, { method: "DELETE" });
      setServerKeys((prev) => prev.filter((item) => item.id !== key.id));
      if (editingServerKey?.id === key.id) {
        setEditingServerKey(null);
      }
      setSuccess(`'${key.name}' 키를 삭제했습니다.`);
    });
  };

  const handleCancelServerKeyEdit = () => {
    setEditingServerKey(null);
  };

  const handleDeleteUser = async (username: string) => {
    if (!confirm(`${username} 사용자를 삭제하시겠습니까?`)) return;
    await runWithStatus(`delete-${username}`, async () => {
      await request(`/api/auth/users/${encodeURIComponent(username)}`, {
        method: "DELETE"
      });
      setUsers((prev) => prev.filter((user) => user.username !== username));
      if (selectedUser?.username === username) {
        setSelectedUser(null);
      }
      setSuccess(`${username} 사용자를 삭제했습니다.`);
    });
  };

  const handleSelectUser = async (username: string) => {
    await runWithStatus(`detail-${username}`, async () => {
      const detail = await request<UserDetail>(`/api/auth/users/${encodeURIComponent(username)}`);
      setSelectedUser(detail);
    });
  };

  const handleIssueToken = async (username: string) => {
    await runWithStatus(`issue-${username}`, async () => {
      const tokens = await request<TokenPayload>("/api/auth/token/issue", {
        method: "POST",
        body: JSON.stringify({ username })
      });
      const issuedAt = new Date().toISOString();
      const payload = { ...tokens, username, issuedAt };
      setTokenCache((prev) => ({ ...prev, [username]: payload }));
      setSuccess(`${username} 사용자에게 토큰이 발급되었습니다.`);
    });
  };

  const handleReissueFromCache = async (username: string) => {
    const cached = tokenCache[username];
    if (!cached?.refreshToken) {
      setError("저장된 리프레시 토큰이 없습니다. 먼저 토큰을 발급해주세요.");
      return;
    }

    await runWithStatus(`reissue-${username}`, async () => {
      const tokens = await request<TokenPayload>("/api/auth/token/re-issue", {
        method: "POST",
        body: JSON.stringify({ username, refreshToken: cached.refreshToken })
      });
      const payload = { ...tokens, username, issuedAt: new Date().toISOString() };
      setTokenCache((prev) => ({ ...prev, [username]: payload }));
      setSuccess(`${username} 사용자의 토큰을 재발급했습니다.`);
    });
  };

  const isBusy = useCallback(
    (label: string) => busyAction === label,
    [busyAction]
  );

  const formattedSelectedUser = useMemo(() => {
    if (!selectedUser) return null;
    return {
      ...selectedUser,
      createdAtFormatted: formatDateTime(selectedUser.createdAt),
      updatedAtFormatted: formatDateTime(selectedUser.updatedAt)
    };
  }, [selectedUser]);

  const copyToClipboard = (value: string) => {
    if (!value) return;
    navigator.clipboard?.writeText(value).then(() => {
      setSuccess("클립보드에 복사했습니다.");
    });
  };

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Shield className="h-4 w-4" />
              서버 인증 키
            </CardTitle>
            <CardDescription>백엔드와 통신할 때 사용할 `X-REGISTRATION-KEY` 값을 저장합니다.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 md:flex-row md:items-center">
          <Input
            value={registrationKey}
            onChange={(event) => setRegistrationKey(event.target.value)}
            placeholder="서버 인증 키를 입력하세요"
          />
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setRegistrationKey((prev) => prev.trim())}
              disabled={!registrationKey}
            >
              공백 제거
            </Button>
            <Button type="button" variant="outline" onClick={() => setRegistrationKey("")}>
              초기화
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <CardTitle className="text-base">서버 인증 키 발급</CardTitle>
            <CardDescription>사용자 등록에 사용할 신규 서버 인증 키를 생성합니다.</CardDescription>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleFetchServerKeys}
            disabled={isBusy("fetchServerKeys")}
          >
            {isBusy("fetchServerKeys") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            목록 새로고침
          </Button>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2">
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">이름 *</p>
            <Input
              value={serverKeyForm.name}
              placeholder="예: Payment-Gateway"
              onChange={(event) => setServerKeyForm((prev) => ({ ...prev, name: event.target.value }))}
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">발급자</p>
            <Input
              value={serverKeyForm.issuedBy}
              placeholder="담당자 이름"
              onChange={(event) => setServerKeyForm((prev) => ({ ...prev, issuedBy: event.target.value }))}
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">만료일 (선택)</p>
            <Input
              type="datetime-local"
              value={serverKeyForm.expiresAt}
              onChange={(event) => setServerKeyForm((prev) => ({ ...prev, expiresAt: event.target.value }))}
            />
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-xs text-slate-500">설명</p>
            <Input
              value={serverKeyForm.description}
              placeholder="비고"
              onChange={(event) => setServerKeyForm((prev) => ({ ...prev, description: event.target.value }))}
            />
          </div>
          <div className="md:col-span-2 flex gap-2">
            <Button type="button" onClick={handleCreateServerKey} disabled={isBusy("createServerKey")}>
              {isBusy("createServerKey") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              서버 인증 키 발급
            </Button>
            <Button type="button" variant="outline" onClick={() => setServerKeyForm({ name: "", issuedBy: "", description: "", expiresAt: "" })}>
              초기화
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">서버 인증 키 목록</CardTitle>
          <CardDescription>발급된 키와 사용 이력을 확인하고 관리합니다.</CardDescription>
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
                {serverKeys.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-6 text-center text-sm text-slate-500">
                      등록된 서버 인증 키가 없습니다. 상단의 발급 폼을 사용해 생성하세요.
                    </TableCell>
                  </TableRow>
                ) : (
                  serverKeys.map((key, index) => (
                    <TableRow key={key.id}>
                      <TableCell className="font-mono text-xs text-slate-500">{serverKeys.length - index}</TableCell>
                      <TableCell className="font-medium">{key.name}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs">{key.keyValue}</span>
                          <Button variant="ghost" size="icon" onClick={() => copyToClipboard(key.keyValue)}>
                            <Copy className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                      <TableCell>{key.issuedBy || "-"}</TableCell>
                      <TableCell>{formatDateTime(key.expiresAt)}</TableCell>
                      <TableCell>
                        <Badge variant={key.active ? "secondary" : "outline"}>
                          {key.active ? "활성" : "비활성"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="수정"
                            onClick={() => handleEditServerKey(key)}
                          >
                            <Search className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-rose-600 hover:text-rose-700"
                            aria-label="삭제"
                            onClick={() => handleDeleteServerKey(key)}
                            disabled={isBusy(`deleteServerKey-${key.id}`)}
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

      {editingServerKey && (
        <Card>
          <CardHeader className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
            <div>
              <CardTitle className="text-base">서버 인증 키 수정</CardTitle>
              <CardDescription>{editingServerKey.name} 설정을 변경합니다.</CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" onClick={handleCancelServerKeyEdit}>
                취소
              </Button>
              <Button
                onClick={handleUpdateServerKey}
                disabled={isBusy(`updateServerKey-${editingServerKey.id}`)}
              >
                {isBusy(`updateServerKey-${editingServerKey.id}`) && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                저장
              </Button>
            </div>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">이름</p>
              <Input
                value={serverKeyEditForm.name}
                onChange={(event) => setServerKeyEditForm((prev) => ({ ...prev, name: event.target.value }))}
              />
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">만료일</p>
              <Input
                type="datetime-local"
                value={serverKeyEditForm.expiresAt}
                onChange={(event) =>
                  setServerKeyEditForm((prev) => ({ ...prev, expiresAt: event.target.value }))
                }
              />
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-slate-500">설명</p>
              <Input
                value={serverKeyEditForm.description}
                onChange={(event) =>
                  setServerKeyEditForm((prev) => ({ ...prev, description: event.target.value }))
                }
              />
            </div>
            <div className="flex items-center gap-2 pt-4">
              <input
                id="server-key-active"
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300"
                checked={serverKeyEditForm.active}
                onChange={(event) =>
                  setServerKeyEditForm((prev) => ({ ...prev, active: event.target.checked }))
                }
              />
              <label htmlFor="server-key-active" className="text-sm text-slate-600">
                활성 상태
              </label>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">사용자 등록</CardTitle>
          <CardDescription>신규 서비스 계정을 추가합니다.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 md:flex-row md:items-center">
          <Input
            value={newUsername}
            placeholder="username (예: my-service)"
            onChange={(event) => setNewUsername(event.target.value)}
          />
          <Button type="button" onClick={handleCreateUser} disabled={isBusy("createUser")}>
            {isBusy("createUser") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            사용자 등록
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="text-base">사용자 목록</CardTitle>
            <CardDescription>/api/auth/users</CardDescription>
          </div>
          <Button type="button" variant="outline" size="sm" onClick={handleFetchUsers} disabled={isBusy("fetchUsers")}>
            {isBusy("fetchUsers") && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            새로고침
          </Button>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16 text-center">No</TableHead>
                  <TableHead>사용자명</TableHead>
                  <TableHead>생성일</TableHead>
                  <TableHead>수정일</TableHead>
                  <TableHead className="w-32 text-center">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="py-6 text-center text-sm text-slate-500">
                      조회된 사용자가 없습니다. 새로고침을 눌러 목록을 불러오세요.
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((user, index) => {
                    const displayNo = users.length - index;
                    return (
                      <TableRow key={user.id ?? user.username}>
                        <TableCell className="text-center font-mono text-xs text-slate-500">{displayNo}</TableCell>
                        <TableCell className="font-medium">{user.username}</TableCell>
                        <TableCell>{formatDateTime(user.createdAt)}</TableCell>
                        <TableCell>{formatDateTime(user.updatedAt)}</TableCell>
                        <TableCell>
                          <div className="flex justify-center gap-2">
                            <Button
                              aria-label="상세 조회"
                              variant="ghost"
                              size="icon"
                              onClick={() => handleSelectUser(user.username)}
                              disabled={isBusy(`detail-${user.username}`)}
                            >
                              <Search className="h-4 w-4" />
                            </Button>
                            <Button
                              aria-label="사용자 삭제"
                              variant="ghost"
                              size="icon"
                              className="text-rose-600 hover:text-rose-700"
                              onClick={() => handleDeleteUser(user.username)}
                              disabled={isBusy(`delete-${user.username}`)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {formattedSelectedUser && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Users className="h-4 w-4" />
              사용자 상세
            </CardTitle>
            <CardDescription>{formattedSelectedUser.username} 계정의 메타데이터</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2">
            <div>
              <p className="text-xs text-slate-500">사용자명</p>
              <p className="font-medium">{formattedSelectedUser.username}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">ID</p>
              <p className="font-mono text-sm">{formattedSelectedUser.id}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">생성일</p>
              <p>{formattedSelectedUser.createdAtFormatted}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">수정일</p>
              <p>{formattedSelectedUser.updatedAtFormatted}</p>
            </div>
            <div className="sm:col-span-2 flex flex-wrap gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => handleIssueToken(formattedSelectedUser.username)}
                disabled={isBusy(`issue-${formattedSelectedUser.username}`)}
              >
                {isBusy(`issue-${formattedSelectedUser.username}`) && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                토큰 발급
              </Button>
              {tokenCache[formattedSelectedUser.username]?.refreshToken && (
                <Button
                  type="button"
                  onClick={() => handleReissueFromCache(formattedSelectedUser.username)}
                  disabled={isBusy(`reissue-${formattedSelectedUser.username}`)}
                >
                  {isBusy(`reissue-${formattedSelectedUser.username}`) && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  토큰 재발급
                </Button>
              )}
            </div>
            {(() => {
              const cached = tokenCache[formattedSelectedUser.username];
              if (!cached) {
                return null;
              }
              return (
                <div className="sm:col-span-2 space-y-3 rounded-lg border border-slate-200 p-3 text-sm dark:border-slate-700">
                  <div className="flex items-center gap-2 text-xs text-slate-500">
                    <Badge variant="secondary">최근 발급</Badge>
                    <span>{new Date(cached.issuedAt).toLocaleString()}</span>
                  </div>
                  <div className="flex flex-col gap-1">
                    <p className="text-xs text-slate-500">Access Token</p>
                    <div className="flex items-center gap-2">
                      <Input value={cached.token} readOnly className="font-mono text-xs" />
                      <Button variant="outline" size="icon" onClick={() => copyToClipboard(cached.token)}>
                        <Copy className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                  <div className="flex flex-col gap-1">
                    <p className="text-xs text-slate-500">Refresh Token</p>
                    <div className="flex items-center gap-2">
                      <Input value={cached.refreshToken} readOnly className="font-mono text-xs" />
                      <Button variant="outline" size="icon" onClick={() => copyToClipboard(cached.refreshToken)}>
                        <Copy className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              );
            })()}
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
