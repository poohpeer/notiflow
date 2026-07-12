import { createBrowserRouter } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { NotificationDetailPage } from './pages/NotificationDetailPage';
import { CreateNotificationPage } from './pages/CreateNotificationPage';

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <Layout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'notifications', element: <NotificationsPage /> },
      // `new` must precede the `:id` param route so it isn't captured as an id.
      { path: 'notifications/new', element: <CreateNotificationPage /> },
      { path: 'notifications/:id', element: <NotificationDetailPage /> },
    ],
  },
]);
