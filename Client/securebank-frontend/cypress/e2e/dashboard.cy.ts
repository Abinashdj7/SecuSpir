describe('Dashboard', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' }).as('getAccounts');
    cy.visitWithAuth('/dashboard');
    cy.wait('@getAccounts');
  });

  it('renders account cards for each account', () => {
    cy.get('[data-cy="account-card"]').should('have.length', 2);
  });

  it('displays the correct total balance', () => {
    // 1500.00 + 3250.75 = 4750.75
    cy.get('[data-cy="total-balance"]').should('contain.text', '4,750.75');
  });

  it('shows both account types', () => {
    cy.get('[data-cy="account-card"]').first().should('contain.text', 'CHECKING');
    cy.get('[data-cy="account-card"]').last().should('contain.text', 'SAVINGS');
  });

  it('opens the new account modal when the button is clicked', () => {
    cy.get('[data-cy="new-account-modal"]').should('not.exist');
    cy.get('[data-cy="new-account-btn"]').click();
    cy.get('[data-cy="new-account-modal"]').should('be.visible');
  });

  it('closes the modal when Cancel is clicked', () => {
    cy.get('[data-cy="new-account-btn"]').click();
    cy.get('[data-cy="new-account-modal"]').should('be.visible');
    cy.get('[data-cy="modal-cancel-btn"]').click();
    cy.get('[data-cy="new-account-modal"]').should('not.exist');
  });

  it('creates a new Checking account from the modal', () => {
    const newAccount = {
      id: 3,
      accountNumber: 'AABBCCDD11223344',
      accountType: 'CHECKING',
      balance: 0,
      status: 'ACTIVE',
    };

    cy.intercept('POST', '**/api/accounts', {
      statusCode: 201,
      body: newAccount,
    }).as('createAccount');

    cy.intercept('GET', '**/api/accounts', {
      body: [
        { id: 1, accountNumber: 'ABCDEF1234567890', accountType: 'CHECKING', balance: 1500, status: 'ACTIVE' },
        { id: 2, accountNumber: '1234567890ABCDEF', accountType: 'SAVINGS', balance: 3250.75, status: 'ACTIVE' },
        newAccount,
      ],
    }).as('getAccountsUpdated');

    cy.get('[data-cy="new-account-btn"]').click();
    cy.get('[data-cy="modal-checking-btn"]').click();

    cy.wait('@createAccount');
    cy.wait('@getAccountsUpdated');
    cy.get('[data-cy="account-card"]').should('have.length', 3);
  });

  it('navigates to transaction history when the account button is clicked', () => {
    cy.intercept('GET', '**/api/accounts/1/transactions', { fixture: 'transactions.json' }).as('getTransactions');
    cy.intercept('GET', '**/api/accounts/1', {
      body: { id: 1, accountNumber: 'ABCDEF1234567890', accountType: 'CHECKING', balance: 1500, status: 'ACTIVE' },
    });

    cy.get('[data-cy="account-history-btn"]').first().click();
    cy.url().should('include', '/accounts/1/transactions');
  });

  it('navigates to /transfer when the Transfer quick action is clicked', () => {
    cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' });
    cy.get('[data-cy="transfer-quick-action"]').click();
    cy.url().should('include', '/transfer');
  });

  it('logs out and redirects to /login', () => {
    cy.get('[data-cy="logout-btn"]').click();
    cy.url().should('include', '/login');
  });

  it('shows an empty state when the user has no accounts', () => {
    cy.intercept('GET', '**/api/accounts', { body: [] }).as('getEmpty');
    cy.visitWithAuth('/dashboard');
    cy.wait('@getEmpty');
    cy.get('[data-cy="empty-state"]').should('be.visible');
  });
});
