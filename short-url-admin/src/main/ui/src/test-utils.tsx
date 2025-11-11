import { ReactNode } from "react";
import { MemoryRouter, MemoryRouterProps } from "react-router-dom";
import { render } from "@testing-library/react";

export function renderWithRouter(
  ui: ReactNode,
  routerProps: Omit<MemoryRouterProps, "children"> = {}
) {
  return render(<MemoryRouter {...routerProps}>{ui}</MemoryRouter>);
}
