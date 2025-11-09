import { render } from "@testing-library/react";
import { RedirectionControlsPage } from "./RedirectionControlsPage";

describe("RedirectionControlsPage", () => {
  it("renders fallback controls", () => {
    const { getByText } = render(<RedirectionControlsPage />);
    expect(getByText("리디렉션 제어")).toBeInTheDocument();
    expect(getByText("응답 정의")).toBeInTheDocument();
  });
});
