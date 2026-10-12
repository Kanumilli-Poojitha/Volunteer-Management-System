// Centralised API client: one place that builds URLs, attaches the bearer token,
// normalises errors and reacts to 401 responses.
//
// Token storage: sessionStorage (cleared when the tab closes). A script injected by an XSS bug could
// read it - see README "Token storage trade-offs". Only the opaque token is stored; the user's
// profile/role is always re-fetched from the server (GET /api/users/me) and never trusted from storage.
// Passwords are never stored.

export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const TOKEN_KEY = 'volunteerhub.token';

export const tokenStore = {
  get() {
    try {
      return window.sessionStorage.getItem(TOKEN_KEY);
    } catch (e) {
      return null;
    }
  },
  set(token) {
    try {
      window.sessionStorage.setItem(TOKEN_KEY, token);
    } catch (e) {
      /* storage unavailable: user simply has to log in again after a reload */
    }
  },
  clear() {
    try {
      window.sessionStorage.removeItem(TOKEN_KEY);
    } catch (e) {
      /* ignore */
    }
  },
};

let unauthorizedHandler = null;

/** AuthContext registers a callback that clears auth state when the server says the token is no good. */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.name = 'ApiError';
    this.status = status; // 0 = network failure
    this.fieldErrors = fieldErrors || null;
  }
}

async function readBody(res) {
  if (res.status === 204) return null;
  try {
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  } catch (e) {
    return null;
  }
}

/**
 * @param {string} path e.g. "/api/users/me"
 * @param {{method?: string, body?: object, auth?: boolean}} options
 *   auth=false for public endpoints (login/register): no token is sent and a 401 is shown to the user
 *   as a normal "invalid credentials" error instead of triggering the session-expired flow.
 */
export async function apiRequest(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = auth ? tokenStore.get() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    throw new ApiError('Cannot reach the server. Check your connection and try again.', 0);
  }

  const data = await readBody(res);
  if (res.ok) return data;

  if (res.status === 401 && auth) {
    tokenStore.clear();
    if (unauthorizedHandler) unauthorizedHandler();
    throw new ApiError('Your session has expired. Please log in again.', 401);
  }
  const message =
    (data && data.message) ||
    (res.status === 401 ? 'Invalid email or password' : 'Something went wrong. Please try again.');
  throw new ApiError(message, res.status, data && data.fieldErrors);
}
