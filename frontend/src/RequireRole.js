import { useAuth } from './AuthContext';

/**
 * UI convenience only. The backend enforces every permission; hiding a screen is not security.
 * Renders children only when a user is logged in and has one of the allowed roles.
 */
export default function RequireRole({ roles, children, fallback = null }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) return fallback;
  return children;
}
