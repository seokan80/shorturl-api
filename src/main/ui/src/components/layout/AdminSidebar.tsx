import { NavLink } from "react-router-dom";
import { FileText, ShieldCheck, Link2, Shuffle, BarChart3, Users, KeyRound } from "lucide-react";
import { cn } from "../../lib/utils";

const navItems = [
  { to: "/server-keys", label: "서버 키 관리", icon: KeyRound },
  { to: "/users", label: "사용자 관리", icon: Users },
  { to: "/short-url", label: "단축 URL 제어", icon: Link2 },
  { to: "/redirection", label: "리디렉션 제어", icon: Shuffle },
  { to: "/analytics", label: "통계 제어", icon: BarChart3 },
  { to: "/specs", label: "API 명세", icon: FileText },
  { to: "/auth", label: "인증 제어", icon: ShieldCheck },
];

export function AdminSidebar() {
  return (
    <aside className="flex h-full flex-col gap-6 border-r border-slate-200 bg-white/90 px-4 py-6 backdrop-blur dark:border-slate-800 dark:bg-slate-950/80">
      <div className="flex items-center gap-2 text-lg font-semibold text-slate-900 dark:text-slate-100">
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
                  : "text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-slate-100"
              )
            }
            end={to === "/"}
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-500 dark:border-slate-800 dark:bg-slate-900/60 dark:text-slate-400">
        Oracle-backed storage. All changes are audited and versioned.
      </div>
    </aside>
  );
}
