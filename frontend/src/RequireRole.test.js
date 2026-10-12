import { render, screen } from '@testing-library/react';
import RequireRole from './RequireRole';
import { AuthProvider } from './AuthContext';
import { tokenStore } from './api/client';
import { admin, jsonResponse, mockFetch, volunteer } from './testUtils';

beforeEach(() => window.sessionStorage.clear());

async function renderWithSessionOf(user) {
  tokenStore.set('t');
  mockFetch({ 'GET /api/users/me': jsonResponse(200, user) });
  render(
    <AuthProvider>
      <RequireRole roles={['ADMIN']} fallback={<p>denied</p>}>
        <p>secret admin area</p>
      </RequireRole>
    </AuthProvider>
  );
  // initial render happens before /me resolves: nobody is logged in yet
  expect(screen.queryByText('secret admin area')).not.toBeInTheDocument();
}

test('renders children for an allowed role', async () => {
  await renderWithSessionOf(admin);
  expect(await screen.findByText('secret admin area')).toBeInTheDocument();
});

test('renders the fallback for a role that is not allowed', async () => {
  await renderWithSessionOf(volunteer);
  expect(await screen.findByText('denied')).toBeInTheDocument();
  expect(screen.queryByText('secret admin area')).not.toBeInTheDocument();
});

test('renders the fallback when nobody is logged in', () => {
  mockFetch({});
  render(
    <AuthProvider>
      <RequireRole roles={['ADMIN']} fallback={<p>denied</p>}><p>secret admin area</p></RequireRole>
    </AuthProvider>
  );
  expect(screen.getByText('denied')).toBeInTheDocument();
});
