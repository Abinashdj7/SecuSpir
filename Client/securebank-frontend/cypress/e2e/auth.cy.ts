import { makeToken } from '../support/commands';

describe('Authentication', () => {
  describe('Login page', () => {
    beforeEach(() => {
      cy.visit('/login');
    });

    it('renders the login form', () => {
      cy.get('[data-cy="email-input"]').should('be.visible');
      cy.get('[data-cy="password-input"]').should('be.visible');
      cy.get('[data-cy="submit-btn"]').should('contain.text', 'Sign in');
    });

    it('shows an error message on invalid credentials', () => {
      cy.intercept('POST', '**/api/auth/login', {
        statusCode: 401,
        body: { error: 'Invalid credentials' },
      }).as('login');

      cy.get('[data-cy="email-input"]').type('wrong@test.com');
      cy.get('[data-cy="password-input"]').type('WrongPass1!');
      cy.get('[data-cy="submit-btn"]').click();

      cy.wait('@login');
      cy.get('[data-cy="error-message"]').should('be.visible');
    });

    it('redirects to /dashboard on successful login', () => {
      const token = makeToken('test@test.com');

      cy.intercept('POST', '**/api/auth/login', {
        statusCode: 200,
        body: { token, email: 'test@test.com', role: 'ROLE_USER' },
      }).as('login');

      cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' }).as('getAccounts');

      cy.get('[data-cy="email-input"]').type('test@test.com');
      cy.get('[data-cy="password-input"]').type('Test1234!');
      cy.get('[data-cy="submit-btn"]').click();

      cy.wait('@login');
      cy.url().should('include', '/dashboard');
    });

    it('navigates to the register page via the link', () => {
      cy.contains('Create one').click();
      cy.url().should('include', '/register');
    });
  });

  describe('Register page', () => {
    beforeEach(() => {
      cy.visit('/register');
    });

    it('renders the registration form', () => {
      cy.get('[data-cy="first-name-input"]').should('be.visible');
      cy.get('[data-cy="last-name-input"]').should('be.visible');
      cy.get('[data-cy="email-input"]').should('be.visible');
      cy.get('[data-cy="password-input"]').should('be.visible');
      cy.get('[data-cy="submit-btn"]').should('contain.text', 'Create account');
    });

    it('redirects to /dashboard on successful registration', () => {
      const token = makeToken('new@test.com');

      cy.intercept('POST', '**/api/auth/register', {
        statusCode: 200,
        body: { token, email: 'new@test.com', role: 'ROLE_USER' },
      }).as('register');

      cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' }).as('getAccounts');

      cy.get('[data-cy="first-name-input"]').type('John');
      cy.get('[data-cy="last-name-input"]').type('Doe');
      cy.get('[data-cy="email-input"]').type('new@test.com');
      cy.get('[data-cy="password-input"]').type('Test1234!');
      cy.get('[data-cy="submit-btn"]').click();

      cy.wait('@register');
      cy.url().should('include', '/dashboard');
    });

    it('shows an error message when registration fails', () => {
      cy.intercept('POST', '**/api/auth/register', {
        statusCode: 409,
        body: { error: 'Registration could not be completed' },
      }).as('register');

      cy.get('[data-cy="first-name-input"]').type('John');
      cy.get('[data-cy="last-name-input"]').type('Doe');
      cy.get('[data-cy="email-input"]').type('existing@test.com');
      cy.get('[data-cy="password-input"]').type('Test1234!');
      cy.get('[data-cy="submit-btn"]').click();

      cy.wait('@register');
      cy.get('[data-cy="error-message"]').should('be.visible');
    });

    it('navigates to the login page via the link', () => {
      cy.contains('Sign in').click();
      cy.url().should('include', '/login');
    });
  });

  describe('Auth guard', () => {
    it('redirects to /login when visiting /dashboard without a token', () => {
      cy.visit('/dashboard');
      cy.url().should('include', '/login');
    });

    it('redirects to /login when visiting /transfer without a token', () => {
      cy.visit('/transfer');
      cy.url().should('include', '/login');
    });

    it('redirects to /login when visiting a transaction page without a token', () => {
      cy.visit('/accounts/1/transactions');
      cy.url().should('include', '/login');
    });
  });
});
