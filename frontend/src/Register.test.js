import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Register from './Register';
import { AuthProvider } from './AuthContext';
import { jsonResponse, mockFetch } from './testUtils';

beforeEach(() => window.sessionStorage.clear());

const renderRegister = () => render(<AuthProvider><Register /></AuthProvider>);

function fillForm({ name = 'Jane Doe', email = 'jane@example.com', password = 'Password123!', role = 'VOLUNTEER' } = {}) {
  if (name) userEvent.type(screen.getByLabelText('Name'), name);
  if (email) userEvent.type(screen.getByLabelText('Email'), email);
  if (password) userEvent.type(screen.getByLabelText('Password'), password);
  if (role) userEvent.selectOptions(screen.getByLabelText('Role'), role);
  userEvent.click(screen.getByRole('button', { name: 'Register' }));
}

test('validates every required field without calling the API', () => {
  const fetchMock = mockFetch({});
  renderRegister();
  userEvent.click(screen.getByRole('button', { name: 'Register' }));
  expect(screen.getByText('Name is required')).toBeInTheDocument();
  expect(screen.getByText('Email is required')).toBeInTheDocument();
  expect(screen.getByText('Password is required')).toBeInTheDocument();
  expect(screen.getByText('Please select a role')).toBeInTheDocument();
  expect(fetchMock).not.toHaveBeenCalled();
});

test('enforces the minimum password length', () => {
  mockFetch({});
  renderRegister();
  fillForm({ password: 'short' });
  expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
});

test('offers only Volunteer and Organizer - Admin is not selectable', () => {
  renderRegister();
  const options = Array.from(screen.getByLabelText('Role').querySelectorAll('option')).map((o) => o.textContent);
  expect(options).toEqual(['Select Role', 'Volunteer', 'Organizer']);
});

test('submits the registration and confirms success', async () => {
  const fetchMock = mockFetch({ 'POST /api/auth/register': jsonResponse(201, { id: 5, email: 'jane@example.com', role: 'ORGANIZER' }) });
  renderRegister();
  fillForm({ role: 'ORGANIZER' });
  expect(await screen.findByRole('status')).toHaveTextContent('Registered successfully');
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    name: 'Jane Doe', email: 'jane@example.com', password: 'Password123!', role: 'ORGANIZER',
  });
  expect(screen.getByLabelText('Password')).toHaveValue('');
});

test('shows the duplicate-email conflict returned by the server', async () => {
  mockFetch({ 'POST /api/auth/register': jsonResponse(409, { message: 'An account with this email already exists' }) });
  renderRegister();
  fillForm();
  expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email already exists');
});
