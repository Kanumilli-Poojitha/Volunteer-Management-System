import { useCallback, useEffect, useState } from 'react';
import { apiRequest } from './api/client';
import { ROLES, roleLabel } from './roles';

const STATUSES = ['ACTIVE', 'DEACTIVATED', 'SUSPENDED'];

function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setUsers(await apiRequest('/api/admin/users'));
      setError('');
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const act = async (fn) => {
    try {
      await fn();
      await load();
    } catch (err) {
      setError(err.message); // e.g. "Cannot demote the last active administrator"
    }
  };

  return (
    <div>
      <h2>User administration</h2>
      {error && <p role="alert" className="form-error">{error}</p>}
      <table>
        <thead>
          <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th /></tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.name}</td>
              <td>{u.email}</td>
              <td>
                <select
                  aria-label={`Role for ${u.email}`}
                  value={u.role}
                  onChange={(e) => act(() => apiRequest(`/api/admin/users/${u.id}/role`, { method: 'PATCH', body: { role: e.target.value } }))}
                >
                  {Object.values(ROLES).map((r) => <option key={r} value={r}>{roleLabel(r)}</option>)}
                </select>
              </td>
              <td>
                <select
                  aria-label={`Status for ${u.email}`}
                  value={u.status}
                  onChange={(e) => act(() => apiRequest(`/api/admin/users/${u.id}/status`, { method: 'PATCH', body: { status: e.target.value } }))}
                >
                  {STATUSES.map((s) => <option key={s} value={s}>{roleLabel(s)}</option>)}
                </select>
              </td>
              <td>
                <button
                  onClick={() => window.confirm(`Delete ${u.email}?`) && act(() => apiRequest(`/api/admin/users/${u.id}`, { method: 'DELETE' }))}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default AdminUsers;
