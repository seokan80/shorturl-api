import { render } from "@testing-library/react";
import { ShortUrlControlsPage } from "./ShortUrlControlsPage";

describe("ShortUrlControlsPage", () => {
  it("shows short url rules and endpoint summary", () => {
    const { getByText } = render(<ShortUrlControlsPage />);
    expect(getByText("단축 URL 제어")).toBeInTheDocument();
    expect(getByText(/엔드포인트 참고/)).toBeInTheDocument();
  });
});
