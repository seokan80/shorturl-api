import { screen } from "@testing-library/react";
import { DashboardPage } from "./DashboardPage";
import { renderWithRouter } from "../../test-utils";

describe("DashboardPage", () => {
  it("renders key metric cards and activity list", () => {
    renderWithRouter(<DashboardPage />);

    expect(screen.getByText("Active Projects")).toBeInTheDocument();
    expect(screen.getByText("Recent Activity")).toBeInTheDocument();
    expect(screen.getByText(/Workflow Highlights/)).toBeInTheDocument();
  });
});
