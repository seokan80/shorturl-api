import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";

const mockFetch = (payload: unknown, ok = true, status = 200) => {
  const fn = vi.fn().mockResolvedValue({
    ok,
    status,
    json: () => Promise.resolve(payload),
  });
  (globalThis as { fetch: unknown }).fetch = fn;
  return fn;
};

describe("apiRequest", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("unwraps the envelope on success", async () => {
    mockFetch({ code: "0000", message: "ok", data: { id: 1 } });

    const result = await apiRequest<{ id: number }>("/api/ping");

    expect(result).toEqual({ id: 1 });
  });

  it("throws with server message on non-success code", async () => {
    mockFetch({ code: "9999", message: "bad", data: null });

    await expect(apiRequest("/api/ping")).rejects.toThrow("bad");
  });

  it("throws when response is not OK even with envelope", async () => {
    mockFetch({ code: "9999", message: "server error", data: null }, false, 500);

    await expect(apiRequest("/api/ping")).rejects.toThrow("server error");
  });

  it("sets JSON content-type automatically when body is provided", async () => {
    const fetchFn = mockFetch({ code: "0000", message: "ok", data: {} });

    await apiRequest("/api/ping", { method: "POST", body: JSON.stringify({ a: 1 }) });

    const init = fetchFn.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Headers;
    expect(headers.get("Content-Type")).toBe("application/json");
  });
});
