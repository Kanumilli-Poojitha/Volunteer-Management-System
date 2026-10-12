import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiRequest, setUnauthorizedHandler, tokenStore } from './api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // safe profile from the server; held in memory only
  const [initializing, setInitializing] = useState(() => Boolean(tokenStore.get()));
  const [notice, setNotice] = useState('');

  const clearAuth = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  // Any 401 from an authenticated request lands here (see api/client.js).
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setNotice('Your session has expired. Please log in again.');
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  // Restore the session after a page reload by asking the server who the token belongs to.
  useEffect(() => {
    if (!tokenStore.get()) return undefined;
    let cancelled = false;
    apiRequest('/api/users/me')
      .then((profile) => !cancelled && setUser(profile))
      .catch(() => !cancelled && clearAuth())
      .finally(() => !cancelled && setInitializing(false));
    return () => {
      cancelled = true;
    };
  }, [clearAuth]);

  const login = useCallback(async (email, password) => {
    const data = await apiRequest('/api/auth/login', {
      method: 'POST',
      body: { email: email.trim(), password },
      auth: false,
    });
    tokenStore.set(data.token);
    setNotice('');
    setUser(data.user);
    return data.user;
  }, []);

  const register = useCallback(
    (form) =>
      apiRequest('/api/auth/register', {
        method: 'POST',
        body: { ...form, name: form.name.trim(), email: form.email.trim() },
        auth: false,
      }),
    []
  );

  const logout = useCallback(() => {
    clearAuth();
    setNotice('');
  }, [clearAuth]);

  const value = useMemo(
    () => ({ user, setUser, initializing, notice, login, register, logout }),
    [user, initializing, notice, login, register, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
