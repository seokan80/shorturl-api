import { NavLink } from "react-router-dom";
import { LayoutDashboard, FolderKanban, FileText, ListChecks, Clock, Settings } from "lucide-react";
import { cn } from "../../lib/utils";

const navItems = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/projects", label: "Projects", icon: FolderKanban },
  { to: "/specs", label: "API Specs", icon: FileText },
  { to: "/workflow", label: "Workflow Inbox", icon: ListChecks },
  { to: "/activity", label: "Activity", icon: Clock },
  { to: "/settings", label: "Settings", icon: Settings }
];

export function AdminSidebar() {
  return (
    <aside className="flex h-full flex-col gap-6 border-r border-slate-800 bg-slate-950/80 px-4 py-6">
      <div className="flex items-center gap-2 text-lg font-semibold text-slate-100">
        <div className="h-3 w-3 rounded-full bg-brand"></div>
        Short URL CMS
      </div>
      <nav className="flex flex-1 flex-col gap-2 text-sm">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 transition-colors",
                isActive
                  ? "bg-brand/10 text-brand"
                  : "text-slate-300 hover:bg-slate-900 hover:text-slate-100"
              )
            }
            end={to === "/"}
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-3 text-xs text-slate-400">
        Oracle-backed storage. All changes are audited and versioned.
      </div>
    </aside>
  );
}
