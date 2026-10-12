import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import { tokenStore } from './api/client';
import { admin, jsonResponse, mockFetch, organizer, volunteer } from './testUtils';

beforeEach(() => {
  window.sessionStorage.clear();
});

async function loginAs(user, extraRoutes = {}) {
  cleanup(); // unmount any previous <App/> so only one login form exists
  window.sessionStorage.clear();
  mockFetch({
    'POST /api/auth/login': jsonResponse(200, { token: 'jwt-token', tokenType: 'Bearer', expiresInSeconds: 3600, user }),
    'GET /api/opportunities': jsonResponse(200, []),
    ...extraRoutes,
  });
  render(<App />);
  userEvent.type(screen.getByLabelText('Email', { selector: '#login-email' }), user.email);
  userEvent.type(screen.getByLabelText('Password', { selector: '#login-password' }), 'Password123!');
  userEvent.click(screen.getByRole('button', { name: 'Login' }));
  await screen.findByRole('heading', { name: 'Profile' });
}

test('logged-out users see the register and login forms, not the profile', () => {
  mockFetch({});
  render(<App />);
  expect(screen.getByRole('heading', { name: 'Register' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(screen.queryByRole('heading', { name: 'Profile' })).not.toBeInTheDocument();
});

test('successful login shows the authenticated state with the server-provided role', async () => {
  await loginAs(volunteer);
  expect(screen.getByText('Role: Volunteer')).toBeInTheDocument();
  expect(screen.getByText(/Vera Volunteer \(Volunteer\)/)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Logout' })).toBeInTheDocument();
  expect(tokenStore.get()).toBe('jwt-token');
});

test('the password is never written to browser storage', async () => {
  await loginAs(volunteer);
  const stored = JSON.stringify({ ...window.sessionStorage, ...window.localStorage });
  expect(stored).not.toContain('Password123!');
});

test('logout clears authentication state and the stored token', async () => {
  await loginAs(volunteer);
  userEvent.click(screen.getByRole('button', { name: 'Logout' }));
  expect(await screen.findByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(screen.queryByRole('heading', { name: 'Profile' })).not.toBeInTheDocument();
  expect(tokenStore.get()).toBeNull();
});

test('volunteers do not get the admin tab; admins do and can load the user list', async () => {
  await loginAs(volunteer);
  expect(screen.queryByRole('button', { name: 'Admin' })).not.toBeInTheDocument();
  userEvent.click(screen.getByRole('button', { name: 'Logout' }));

  await loginAs(admin, { 'GET /api/admin/users': jsonResponse(200, [volunteer, admin]) });
  userEvent.click(screen.getByRole('button', { name: 'Admin' }));
  expect(await screen.findByRole('heading', { name: 'User administration' })).toBeInTheDocument();
  expect(await screen.findByText('vera@example.com')).toBeInTheDocument();
});

test('only organizers see the "Post an opportunity" form', async () => {
  await loginAs(volunteer);
  userEvent.click(screen.getByRole('button', { name: 'Opportunities' }));
  await screen.findByRole('heading', { name: 'Opportunities' });
  expect(screen.queryByText('Post an opportunity')).not.toBeInTheDocument();
  userEvent.click(screen.getByRole('button', { name: 'Logout' }));

  await loginAs(organizer);
  userEvent.click(screen.getByRole('button', { name: 'Opportunities' }));
  expect(await screen.findByText('Post an opportunity')).toBeInTheDocument();
});

test('a 401 from an authenticated request clears auth state and returns to login with a notice', async () => {
  await loginAs(volunteer, { 'GET /api/opportunities': jsonResponse(401, { message: 'expired' }) });
  userEvent.click(screen.getByRole('button', { name: 'Opportunities' }));
  expect(await screen.findByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(screen.getByText('Your session has expired. Please log in again.')).toBeInTheDocument();
  expect(tokenStore.get()).toBeNull();
});

test('an existing token restores the session through GET /api/users/me', async () => {
  tokenStore.set('persisted-token');
  const fetchMock = mockFetch({ 'GET /api/users/me': jsonResponse(200, organizer) });
  render(<App />);
  expect(await screen.findByText('Role: Organizer')).toBeInTheDocument();
  expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer persisted-token');
});

test('an invalid stored token is discarded and the login form is shown', async () => {
  tokenStore.set('stale-token');
  mockFetch({ 'GET /api/users/me': jsonResponse(401, { message: 'nope' }) });
  render(<App />);
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Login' })).toBeInTheDocument());
  expect(tokenStore.get()).toBeNull();
});
