export type ApiEnvelope<T> = {
  code: string;
  message: string;
  data: T;
};

const SUCCESS_CODE = "0000";

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;

  if (!payload) {
    throw new Error("응답을 파싱할 수 없습니다.");
  }

  if (!response.ok || payload.code !== SUCCESS_CODE) {
    throw new Error(payload.message ?? "요청에 실패했습니다.");
  }

  return payload.data;
}
