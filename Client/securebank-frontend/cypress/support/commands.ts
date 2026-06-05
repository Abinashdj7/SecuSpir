// Creates a fake but structurally valid JWT whose payload can be decoded by atob().
// The signature is fake so it won't pass backend verification, but the frontend
// only reads the payload to check the expiry — it never re-verifies the signature.
export function makeToken(email = 'test@test.com'): string {
  const exp = Math.floor(Date.now() / 1000) + 3600;
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: email, exp }));
  return `${header}.${payload}.fakesig`;
}

// Visits a protected route while injecting a valid-looking token so the auth
// guard lets the page load rather than redirecting to /login.
Cypress.Commands.add('visitWithAuth', (path: string, email = 'test@test.com') => {
  const token = makeToken(email);
  cy.visit(path, {
    onBeforeLoad(win) {
      win.localStorage.setItem('token', token);
    },
  });
});
