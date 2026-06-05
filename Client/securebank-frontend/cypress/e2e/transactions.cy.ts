const ACCOUNT = {
  id: 1,
  accountNumber: 'ABCDEF1234567890',
  accountType: 'CHECKING',
  balance: 1500.0,
  status: 'ACTIVE',
};

describe('Transaction history page', () => {
  describe('with transactions', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/api/accounts/1', { body: ACCOUNT }).as('getAccount');
      cy.intercept('GET', '**/api/accounts/1/transactions', { fixture: 'transactions.json' }).as('getTransactions');
      cy.visitWithAuth('/accounts/1/transactions');
      cy.wait('@getAccount');
      cy.wait('@getTransactions');
    });

    it('renders the account summary card', () => {
      cy.get('[data-cy="account-summary"]').should('be.visible');
      cy.get('[data-cy="account-summary"]').should('contain.text', 'CHECKING');
      cy.get('[data-cy="account-summary"]').should('contain.text', '1,500.00');
      cy.get('[data-cy="account-summary"]').should('contain.text', 'ABCDEF1234567890');
    });

    it('renders one row per transaction', () => {
      cy.get('[data-cy="transaction-item"]').should('have.length', 3);
    });

    it('shows DEPOSIT, WITHDRAWAL and TRANSFER transaction types', () => {
      cy.get('[data-cy="transaction-item"]').eq(0).should('contain.text', 'Deposit');
      cy.get('[data-cy="transaction-item"]').eq(1).should('contain.text', 'Withdrawal');
      cy.get('[data-cy="transaction-item"]').eq(2).should('contain.text', 'Transfer');
    });

    it('shows the correct amounts', () => {
      cy.get('[data-cy="transaction-item"]').eq(0).should('contain.text', '500.00');
      cy.get('[data-cy="transaction-item"]').eq(1).should('contain.text', '100.00');
      cy.get('[data-cy="transaction-item"]').eq(2).should('contain.text', '200.00');
    });

    it('shows COMPLETED status badge on each transaction', () => {
      cy.get('[data-cy="transaction-item"]').each(($row) => {
        cy.wrap($row).should('contain.text', 'COMPLETED');
      });
    });

    it('navigates back to /dashboard when Back is clicked', () => {
      cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' });
      cy.get('[data-cy="back-btn"]').click();
      cy.url().should('include', '/dashboard');
    });
  });

  describe('with no transactions', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/api/accounts/1', { body: ACCOUNT }).as('getAccount');
      cy.intercept('GET', '**/api/accounts/1/transactions', { body: [] }).as('getEmpty');
      cy.visitWithAuth('/accounts/1/transactions');
      cy.wait('@getAccount');
      cy.wait('@getEmpty');
    });

    it('shows the empty state message', () => {
      cy.get('[data-cy="empty-state"]').should('be.visible');
      cy.get('[data-cy="empty-state"]').should('contain.text', 'No transactions yet');
    });

    it('does not render any transaction rows', () => {
      cy.get('[data-cy="transaction-item"]').should('not.exist');
    });
  });
});
