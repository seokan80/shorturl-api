import { screen } from "@testing-library/react";
import { WorkflowInboxPage } from "./WorkflowInboxPage";
import { renderWithRouter } from "../../test-utils";

describe("WorkflowInboxPage", () => {
  it("displays workflow table with actions", () => {
    renderWithRouter(<WorkflowInboxPage />);

    expect(screen.getByText("Workflow Inbox")).toBeInTheDocument();
    expect(screen.getByText("/api/short-url (POST)")).toBeInTheDocument();
    expect(screen.getAllByText("Approve")[0]).toBeInTheDocument();
  });
});
