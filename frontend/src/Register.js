import { useState } from 'react';
import { useAuth } from './AuthContext';
import { validateRegistration } from './validation';

const EMPTY = { name: '', email: '', password: '', role: '' };

function Register() {
  const { register } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setSuccess('');
    const found = validateRegistration(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    try {
      await register(form);
      setSuccess('Registered successfully. You can now log in.');
      setForm(EMPTY);
    } catch (err) {
      const detail = err.fieldErrors ? ` ${Object.values(err.fieldErrors).join(' ')}` : '';
      setServerError(`${err.message}${err.fieldErrors ? ':' : ''}${detail}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate aria-labelledby="register-heading">
      <h2 id="register-heading">Register</h2>
      <label htmlFor="reg-name">Name</label>
      <input id="reg-name" value={form.name} onChange={update('name')} />
      {errors.name && <p className="field-error">{errors.name}</p>}
      <label htmlFor="reg-email">Email</label>
      <input id="reg-email" type="email" autoComplete="email" value={form.email} onChange={update('email')} />
      {errors.email && <p className="field-error">{errors.email}</p>}
      <label htmlFor="reg-password">Password</label>
      <input id="reg-password" type="password" autoComplete="new-password" value={form.password} onChange={update('password')} />
      {errors.password && <p className="field-error">{errors.password}</p>}
      <label htmlFor="reg-role">Role</label>
      <select id="reg-role" value={form.role} onChange={update('role')}>
        <option value="">Select Role</option>
        <option value="VOLUNTEER">Volunteer</option>
        <option value="ORGANIZER">Organizer</option>
      </select>
      {errors.role && <p className="field-error">{errors.role}</p>}
      {serverError && <p role="alert" className="form-error">{serverError}</p>}
      {success && <p role="status" className="success">{success}</p>}
      <button type="submit" disabled={submitting}>{submitting ? 'Registering…' : 'Register'}</button>
    </form>
  );
}

export default Register;
