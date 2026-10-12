import { useState } from 'react';
import { useAuth } from './AuthContext';
import { validateLogin } from './validation';

function Login() {
  const { login, notice } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    const found = validateLogin(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    try {
      await login(form.email, form.password);
    } catch (err) {
      setServerError(err.message);
      setForm((f) => ({ ...f, password: '' }));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate aria-labelledby="login-heading">
      <h2 id="login-heading">Login</h2>
      {notice && <p role="status" className="notice">{notice}</p>}
      <label htmlFor="login-email">Email</label>
      <input id="login-email" type="email" autoComplete="username" value={form.email} onChange={update('email')} />
      {errors.email && <p className="field-error">{errors.email}</p>}
      <label htmlFor="login-password">Password</label>
      <input id="login-password" type="password" autoComplete="current-password" value={form.password} onChange={update('password')} />
      {errors.password && <p className="field-error">{errors.password}</p>}
      {serverError && <p role="alert" className="form-error">{serverError}</p>}
      <button type="submit" disabled={submitting}>{submitting ? 'Logging in…' : 'Login'}</button>
    </form>
  );
}

export default Login;
