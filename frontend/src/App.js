import { useState } from 'react';
import './App.css';
import { AuthProvider, useAuth } from './AuthContext';
import Register from './Register';
import Login from './Login';
import Profile from './Profile';
import Opportunities from './Opportunities';
import AdminUsers from './AdminUsers';
import RequireRole from './RequireRole';
import { ROLES, roleLabel } from './roles';

function Shell() {
  const { user, initializing, logout } = useAuth();
  const [tab, setTab] = useState('profile');

  if (initializing) return <p>Loading…</p>;

  if (!user) {
    return (
      <div className="auth-screen">
        <Register />
        <Login />
      </div>
    );
  }

  return (
    <div>
      <header>
        <strong>VolunteerHub</strong>
        <span aria-label="current user"> {user.name} ({roleLabel(user.role)}) </span>
        <nav>
          <button onClick={() => setTab('profile')}>Profile</button>
          <button onClick={() => setTab('opportunities')}>Opportunities</button>
          <RequireRole roles={[ROLES.ADMIN]}>
            <button onClick={() => setTab('admin')}>Admin</button>
          </RequireRole>
          <button onClick={logout}>Logout</button>
        </nav>
      </header>
      {tab === 'profile' && <Profile />}
      {tab === 'opportunities' && <Opportunities />}
      {tab === 'admin' && (
        <RequireRole roles={[ROLES.ADMIN]} fallback={<p role="alert">You do not have access to this page.</p>}>
          <AdminUsers />
        </RequireRole>
      )}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  );
}

export default App;
