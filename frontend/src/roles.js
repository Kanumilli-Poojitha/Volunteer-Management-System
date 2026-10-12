export const ROLES = { ADMIN: 'ADMIN', ORGANIZER: 'ORGANIZER', VOLUNTEER: 'VOLUNTEER' };

export const roleLabel = (role) =>
  role ? role.charAt(0) + role.slice(1).toLowerCase() : '';
