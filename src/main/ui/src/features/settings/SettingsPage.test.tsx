import { screen } from "@testing-library/react";
import { SettingsPage } from "./SettingsPage";
import { renderWithRouter } from "../../test-utils";

describe("SettingsPage", () => {
  it("renders integration settings cards", () => {
    renderWithRouter(<SettingsPage />);

    expect(screen.getByText("Integrations")).toBeInTheDocument();
    expect(screen.getByDisplayValue("#api-cms-alerts")).toBeInTheDocument();
    expect(screen.getByText("Oracle Data Sync")).toBeInTheDocument();
  });
});
