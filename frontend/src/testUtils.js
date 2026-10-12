// Helpers for mocking fetch in component tests (no live backend required).
export function jsonResponse(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    text: async () => (body === undefined ? '' : JSON.stringify(body)),
  };
}

/** Route-based fetch mock: routes = { 'POST /api/auth/login': (url, init) => response | [responses] } */
export function mockFetch(routes) {
  global.fetch = jest.fn(async (url, init = {}) => {
    const path = String(url).replace(/^https?:\/\/[^/]+/, '');
    const key = `${init.method || 'GET'} ${path}`;
    const handler = routes[key];
    if (!handler) throw new Error(`Unexpected request: ${key}`);
    return typeof handler === 'function' ? handler(url, init) : handler;
  });
  return global.fetch;
}

export const volunteer = { id: 1, name: 'Vera Volunteer', email: 'vera@example.com', role: 'VOLUNTEER', status: 'ACTIVE' };
export const admin = { id: 2, name: 'Ada Admin', email: 'ada@example.com', role: 'ADMIN', status: 'ACTIVE' };
export const organizer = { id: 3, name: 'Omar Organizer', email: 'omar@example.com', role: 'ORGANIZER', status: 'ACTIVE' };
