import { render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { ShortUrlManagementPage } from "./ShortUrlManagementPage";

describe("ShortUrlManagementPage", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => ({
        code: "0000",
        message: "OK",
        data: { totalCount: 0, elements: [] }
      })
    } as Response);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders management cards", async () => {
    render(<ShortUrlManagementPage />);
    // 고유한 카드 제목으로 검증한다 ("단축 URL 생성" 은 제목과 제출 버튼 양쪽에 있어 모호함).
    expect(await screen.findByText("Short Key 로 조회")).toBeInTheDocument();
    expect(await screen.findByText("단축 URL 목록")).toBeInTheDocument();
  });
});
