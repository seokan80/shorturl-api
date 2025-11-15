import { screen } from "@testing-library/react";
import { Routes, Route } from "react-router-dom";
import { SpecDetailPage } from "./SpecDetailPage";
import { renderWithRouter } from "../../test-utils";

describe("SpecDetailPage", () => {
  it("renders spec detail when id is valid", () => {
    renderWithRouter(
      <Routes>
        <Route path="/specs/:specId" element={<SpecDetailPage />} />
      </Routes>,
      { initialEntries: ["/specs/user-register"] }
    );

    expect(screen.getByText("/api/users")).toBeInTheDocument();
    expect(screen.getByText("Owner · 플랫폼 스쿼드")).toBeInTheDocument();
    expect(screen.getByText("Headers")).toBeInTheDocument();
  });

  it("handles missing spec gracefully", () => {
    renderWithRouter(
      <Routes>
        <Route path="/specs/:specId" element={<SpecDetailPage />} />
      </Routes>,
      { initialEntries: ["/specs/not-real"] }
    );

    expect(screen.getByText("Spec Not Found")).toBeInTheDocument();
  });
});
