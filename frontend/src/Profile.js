import { useState } from 'react';
import { apiRequest } from './api/client';
import { useAuth } from './AuthContext';
import { roleLabel } from './roles';

function Profile() {
  const { user, setUser } = useAuth();
  const [name, setName] = useState(user.name);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const save = async (e) => {
    e.preventDefault();
    setMessage('');
    setError('');
    if (!name.trim()) {
      setError('Name is required');
      return;
    }
    try {
      // Only the name is sent: role/status/email can't be changed here (and the server ignores them anyway).
      const updated = await apiRequest('/api/users/me', { method: 'PUT', body: { name: name.trim() } });
      setUser(updated);
      setMessage('Profile updated');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h2>Profile</h2>
      <p>Name: {user.name}</p>
      <p>Email: {user.email}</p>
      <p>Role: {roleLabel(user.role)}</p>
      <form onSubmit={save} noValidate>
        <label htmlFor="profile-name">Display name</label>
        <input id="profile-name" value={name} onChange={(e) => setName(e.target.value)} />
        <button type="submit">Save</button>
      </form>
      {message && <p role="status" className="success">{message}</p>}
      {error && <p role="alert" className="form-error">{error}</p>}
    </div>
  );
}

export default Profile;
