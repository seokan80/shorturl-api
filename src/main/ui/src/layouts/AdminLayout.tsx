import { Outlet } from "react-router-dom";
import { AdminSidebar } from "../components/layout/AdminSidebar";
import { AdminTopbar } from "../components/layout/AdminTopbar";

export function AdminLayout() {
  return (
    <div className="grid min-h-screen grid-cols-[240px_1fr] bg-slate-950 text-slate-100">
      <AdminSidebar />
      <div className="flex min-h-screen flex-col">
        <AdminTopbar />
        <main className="flex-1 overflow-y-auto p-6">
          <div className="mx-auto flex w-full max-w-6xl flex-col gap-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
