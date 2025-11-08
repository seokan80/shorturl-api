import { Routes, Route } from "react-router-dom";
import { AdminLayout } from "./layouts/AdminLayout";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { ProjectsPage } from "./features/projects/ProjectsPage";
import { SpecsOverviewPage } from "./features/specs/SpecsOverviewPage";
import { SpecDetailPage } from "./features/specs/SpecDetailPage";
import { WorkflowInboxPage } from "./features/workflow/WorkflowInboxPage";
import { ActivityPage } from "./features/activity/ActivityPage";
import { SettingsPage } from "./features/settings/SettingsPage";

function App() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="specs">
          <Route index element={<SpecsOverviewPage />} />
          <Route path=":specId" element={<SpecDetailPage />} />
        </Route>
        <Route path="workflow" element={<WorkflowInboxPage />} />
        <Route path="activity" element={<ActivityPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  );
}

export default App;
