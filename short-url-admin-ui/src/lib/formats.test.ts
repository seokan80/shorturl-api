import { describe, expect, it, vi, afterEach } from "vitest";
import { formatDateTime, isExpired, toInputDateTime, toServerDateTime } from "./formats";

describe("formats", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("formatDateTime returns dash for null/undefined", () => {
    expect(formatDateTime(null)).toBe("-");
    expect(formatDateTime(undefined)).toBe("-");
    expect(formatDateTime("")).toBe("-");
  });

  it("formatDateTime formats a valid ISO string", () => {
    const result = formatDateTime("2025-01-02T03:04:05");
    expect(result).not.toBe("-");
    expect(result.length).toBeGreaterThan(0);
  });

  it("toInputDateTime slices to 16 chars", () => {
    expect(toInputDateTime("2025-12-31T23:59:59")).toBe("2025-12-31T23:59");
    expect(toInputDateTime(null)).toBe("");
  });

  it("toServerDateTime appends :00 when 16 chars", () => {
    expect(toServerDateTime("2025-12-31T23:59")).toBe("2025-12-31T23:59:00");
    expect(toServerDateTime("2025-12-31T23:59:30")).toBe("2025-12-31T23:59:30");
  });

  it("isExpired compares against now", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2025-06-15T00:00:00Z"));
    expect(isExpired("2025-01-01T00:00:00Z")).toBe(true);
    expect(isExpired("2030-01-01T00:00:00Z")).toBe(false);
    expect(isExpired(null)).toBe(false);
  });
});
