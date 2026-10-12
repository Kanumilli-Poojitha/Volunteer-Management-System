import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Login from './Login';
import { AuthProvider } from './AuthContext';
import { jsonResponse, mockFetch, volunteer } from './testUtils';

beforeEach(() => window.sessionStorage.clear());

const renderLogin = () => render(<AuthProvider><Login /></AuthProvider>);
const fill = (email, password) => {
  if (email) userEvent.type(screen.getByLabelText('Email'), email);
  if (password) userEvent.type(screen.getByLabelText('Password'), password);
  userEvent.click(screen.getByRole('button', { name: 'Login' }));
};

test('shows validation errors and does not call the API when the form is empty', () => {
  const fetchMock = mockFetch({});
  renderLogin();
  fill('', '');
  expect(screen.getByText('Email is required')).toBeInTheDocument();
  expect(screen.getByText('Password is required')).toBeInTheDocument();
  expect(fetchMock).not.toHaveBeenCalled();
});

test('rejects a malformed email address before calling the API', () => {
  const fetchMock = mockFetch({});
  renderLogin();
  fill('not-an-email', 'secret123');
  expect(screen.getByText('Enter a valid email address')).toBeInTheDocument();
  expect(fetchMock).not.toHaveBeenCalled();
});

test('shows the generic invalid-credentials message on a 401 and clears the password field', async () => {
  mockFetch({ 'POST /api/auth/login': jsonResponse(401, { message: 'Invalid email or password' }) });
  renderLogin();
  fill('vera@example.com', 'wrongpass1');
  expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  expect(screen.getByLabelText('Password')).toHaveValue('');
});

test('shows a friendly message when the server cannot be reached', async () => {
  global.fetch = jest.fn().mockRejectedValue(new TypeError('Failed to fetch'));
  renderLogin();
  fill('vera@example.com', 'secret123');
  expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
});

test('sends the credentials once and stores only the token on success', async () => {
  const fetchMock = mockFetch({
    'POST /api/auth/login': jsonResponse(200, { token: 'abc', tokenType: 'Bearer', expiresInSeconds: 3600, user: volunteer }),
  });
  renderLogin();
  fill(' vera@example.com ', 'secret123');
  await waitFor(() => expect(window.sessionStorage.getItem('volunteerhub.token')).toBe('abc'));
  expect(fetchMock).toHaveBeenCalledTimes(1);
  const [, init] = fetchMock.mock.calls[0];
  expect(JSON.parse(init.body)).toEqual({ email: 'vera@example.com', password: 'secret123' });
  expect(init.headers.Authorization).toBeUndefined();
});
