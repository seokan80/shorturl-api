import { Routes, Route } from "react-router-dom";
import { AdminLayout } from "./layouts/AdminLayout";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { ProjectsPage } from "./features/projects/ProjectsPage";
import { SpecsOverviewPage } from "./features/specs/SpecsOverviewPage";
import { SpecDetailPage } from "./features/specs/SpecDetailPage";
import { WorkflowInboxPage } from "./features/workflow/WorkflowInboxPage";
import { ActivityPage } from "./features/activity/ActivityPage";
import { SettingsPage } from "./features/settings/SettingsPage";

import { AuthControlsPage } from "./features/controls/AuthControlsPage";
import { ShortUrlManagementPage } from "./features/shorturl/ShortUrlManagementPage";
import { RedirectionControlsPage } from "./features/controls/RedirectionControlsPage";
import { AnalyticsControlsPage } from "./features/controls/AnalyticsControlsPage";
import { UserManagementPage } from "./features/auth/UserManagementPage";
import { ClientAccessKeyPage } from "./features/auth/ClientAccessKeyPage";

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
        <Route path="auth" element={<AuthControlsPage />} />
        <Route path="users" element={<UserManagementPage />} />
        <Route path="client-keys" element={<ClientAccessKeyPage />} />
        <Route path="short-url" element={<ShortUrlManagementPage />} />
        <Route path="redirection" element={<RedirectionControlsPage />} />
        <Route path="analytics" element={<AnalyticsControlsPage />} />
        <Route path="workflow" element={<WorkflowInboxPage />} />
        <Route path="activity" element={<ActivityPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  );
}

export default App;
