declare namespace Cypress {
  interface Chainable {
    /**
     * Visits a route and injects a fake JWT into localStorage before load,
     * so the Angular auth guard allows access.
     */
    visitWithAuth(path: string, email?: string): Chainable<void>;
  }
}
