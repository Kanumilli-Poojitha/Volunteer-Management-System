import { apiRequest, ApiError, setUnauthorizedHandler, tokenStore } from './client';
import { jsonResponse, mockFetch } from '../testUtils';

beforeEach(() => {
  window.sessionStorage.clear();
  setUnauthorizedHandler(null);
});

test('attaches the bearer token to authenticated requests', async () => {
  tokenStore.set('tok');
  const fetchMock = mockFetch({ 'GET /api/users/me': jsonResponse(200, { id: 1 }) });
  await apiRequest('/api/users/me');
  expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer tok');
});

test('does not send the token to public endpoints (auth: false)', async () => {
  tokenStore.set('tok');
  const fetchMock = mockFetch({ 'POST /api/auth/login': jsonResponse(200, {}) });
  await apiRequest('/api/auth/login', { method: 'POST', body: {}, auth: false });
  expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined();
});

test('a 401 on an authenticated request clears the token and calls the unauthorized handler', async () => {
  tokenStore.set('tok');
  const handler = jest.fn();
  setUnauthorizedHandler(handler);
  mockFetch({ 'GET /api/users/me': jsonResponse(401, { message: 'x' }) });
  await expect(apiRequest('/api/users/me')).rejects.toMatchObject({ status: 401 });
  expect(tokenStore.get()).toBeNull();
  expect(handler).toHaveBeenCalledTimes(1);
});

test('a 401 from login is a normal error and does not trigger the session-expired flow', async () => {
  const handler = jest.fn();
  setUnauthorizedHandler(handler);
  mockFetch({ 'POST /api/auth/login': jsonResponse(401, { message: 'Invalid email or password' }) });
  await expect(apiRequest('/api/auth/login', { method: 'POST', body: {}, auth: false })).rejects.toThrow('Invalid email or password');
  expect(handler).not.toHaveBeenCalled();
});

test('a 403 keeps the session and surfaces the server message', async () => {
  tokenStore.set('tok');
  mockFetch({ 'GET /api/admin/users': jsonResponse(403, { message: 'You do not have permission to perform this action' }) });
  await expect(apiRequest('/api/admin/users')).rejects.toMatchObject({ status: 403 });
  expect(tokenStore.get()).toBe('tok');
});

test('network failures become an ApiError with status 0', async () => {
  global.fetch = jest.fn().mockRejectedValue(new TypeError('Failed to fetch'));
  const err = await apiRequest('/api/users/me').catch((e) => e);
  expect(err).toBeInstanceOf(ApiError);
  expect(err.status).toBe(0);
});

test('204 responses resolve to null', async () => {
  mockFetch({ 'DELETE /api/admin/users/1': jsonResponse(204) });
  await expect(apiRequest('/api/admin/users/1', { method: 'DELETE' })).resolves.toBeNull();
});
