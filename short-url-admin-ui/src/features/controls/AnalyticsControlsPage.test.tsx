import { render } from "@testing-library/react";
import { AnalyticsControlsPage } from "./AnalyticsControlsPage";

describe("AnalyticsControlsPage", () => {
  it("renders groupBy options", () => {
    const { getByText } = render(<AnalyticsControlsPage />);
    expect(getByText("통계 제어")).toBeInTheDocument();
    expect(getByText("샘플 응답")).toBeInTheDocument();
  });
});
