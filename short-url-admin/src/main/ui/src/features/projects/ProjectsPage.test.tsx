import { screen } from "@testing-library/react";
import { ProjectsPage } from "./ProjectsPage";
import { renderWithRouter } from "../../test-utils";

describe("ProjectsPage", () => {
  it("shows project table and summary stats", () => {
    renderWithRouter(<ProjectsPage />);

    expect(screen.getByText("Project Overview")).toBeInTheDocument();
    expect(screen.getByText("Short URL API")).toBeInTheDocument();
    expect(screen.getByText(/Drafts In Progress/)).toBeInTheDocument();
  });
});
