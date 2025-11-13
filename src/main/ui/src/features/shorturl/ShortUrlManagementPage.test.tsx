import { render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { ShortUrlManagementPage } from "./ShortUrlManagementPage";

describe("ShortUrlManagementPage", () => {
  beforeEach(() => {
    vi.spyOn(global, "fetch").mockResolvedValue({
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
    expect(await screen.findByText("단축 URL 관리")).toBeInTheDocument();
    expect(await screen.findByText("단축 URL 목록")).toBeInTheDocument();
  });
});
