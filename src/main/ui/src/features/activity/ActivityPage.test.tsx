import { screen } from "@testing-library/react";
import { ActivityPage } from "./ActivityPage";
import { renderWithRouter } from "../../test-utils";

describe("ActivityPage", () => {
  it("shows activity log and comment form", () => {
    renderWithRouter(<ActivityPage />);

    expect(screen.getByText("Comment Threads")).toBeInTheDocument();
    expect(screen.getByText("Add Comment")).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Share feedback/i)).toBeInTheDocument();
  });
});
