import { render } from "@testing-library/react";
import { AuthControlsPage } from "./AuthControlsPage";

describe("AuthControlsPage", () => {
  it("renders header and inputs", () => {
    const { getByText } = render(<AuthControlsPage />);
    expect(getByText("인증 제어")).toBeInTheDocument();
    expect(getByText("헤더 요구사항")).toBeInTheDocument();
  });
});
