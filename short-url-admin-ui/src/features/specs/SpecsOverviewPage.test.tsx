import { screen } from "@testing-library/react";
import { SpecsOverviewPage } from "./SpecsOverviewPage";
import { renderWithRouter } from "../../test-utils";

describe("SpecsOverviewPage", () => {
  it("lists categories and spec cards", () => {
    renderWithRouter(<SpecsOverviewPage />);

    expect(screen.getByText("API Specifications")).toBeInTheDocument();
    expect(screen.getAllByText("단축 URL")[0]).toBeInTheDocument();
    expect(screen.getByText("/api/short-url")).toBeInTheDocument();
  });
});
