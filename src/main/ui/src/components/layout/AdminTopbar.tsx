import { Bell, Search } from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ThemeToggle } from "./ThemeToggle";

export function AdminTopbar() {
  return (
    <header className="flex items-center justify-between gap-4 border-b border-slate-200 bg-white/80 px-6 py-4 backdrop-blur dark:border-slate-800 dark:bg-slate-950/60">
      <div className="flex flex-1 items-center gap-3">
        <div className="relative w-full max-w-md">
          <Input placeholder="Search specs, projects, assignees..." />
          <Search className="pointer-events-none absolute right-3 top-2.5 h-4 w-4 text-slate-400 dark:text-slate-500" />
        </div>
      </div>
      <div className="flex items-center gap-3">
        <ThemeToggle />
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-4 w-4" />
        </Button>
        <span className="flex items-center gap-2 rounded-full border border-slate-200 bg-white/80 px-3 py-1.5 text-sm text-slate-800 dark:border-slate-800 dark:bg-slate-900/70 dark:text-slate-300">
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
          seokan@nh
        </span>
      </div>
    </header>
  );
}
