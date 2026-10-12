import { useCallback, useEffect, useState } from 'react';
import { apiRequest } from './api/client';
import { useAuth } from './AuthContext';
import RequireRole from './RequireRole';
import { ROLES } from './roles';

function Opportunities() {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ title: '', description: '', location: '', eventDate: '' });

  const load = useCallback(async () => {
    try {
      setItems(await apiRequest('/api/opportunities'));
      setError('');
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const create = async (e) => {
    e.preventDefault();
    try {
      await apiRequest('/api/opportunities', { method: 'POST', body: form });
      setForm({ title: '', description: '', location: '', eventDate: '' });
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const remove = async (id) => {
    try {
      await apiRequest(`/api/opportunities/${id}`, { method: 'DELETE' });
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <div>
      <h2>Opportunities</h2>
      {error && <p role="alert" className="form-error">{error}</p>}
      <ul>
        {items.map((o) => (
          <li key={o.id}>
            <strong>{o.title}</strong> — {o.location} on {o.eventDate} (by {o.organizerName})
            {(o.organizerId === user.id || user.role === ROLES.ADMIN) && user.role !== ROLES.VOLUNTEER && (
              <button onClick={() => remove(o.id)}>Delete</button>
            )}
          </li>
        ))}
        {items.length === 0 && !error && <li>No opportunities yet.</li>}
      </ul>
      <RequireRole roles={[ROLES.ORGANIZER]}>
        <form onSubmit={create}>
          <h3>Post an opportunity</h3>
          <label htmlFor="opp-title">Title</label>
          <input id="opp-title" required value={form.title} onChange={set('title')} />
          <label htmlFor="opp-desc">Description</label>
          <input id="opp-desc" value={form.description} onChange={set('description')} />
          <label htmlFor="opp-loc">Location</label>
          <input id="opp-loc" required value={form.location} onChange={set('location')} />
          <label htmlFor="opp-date">Date</label>
          <input id="opp-date" type="date" required value={form.eventDate} onChange={set('eventDate')} />
          <button type="submit">Create</button>
        </form>
      </RequireRole>
    </div>
  );
}

export default Opportunities;
