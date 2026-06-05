describe('Transfer page', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' }).as('getAccounts');
    cy.visitWithAuth('/transfer');
    cy.wait('@getAccounts');
  });

  it('shows the Deposit tab by default', () => {
    cy.get('[data-cy="deposit-tab"]').should('have.class', 'bg-blue-600');
    cy.get('[data-cy="deposit-account-select"]').should('be.visible');
    cy.get('[data-cy="deposit-amount-input"]').should('be.visible');
    cy.get('[data-cy="deposit-submit"]').should('contain.text', 'Deposit');
  });

  it('switches to the Withdraw tab', () => {
    cy.get('[data-cy="withdraw-tab"]').click();
    cy.get('[data-cy="withdraw-account-select"]').should('be.visible');
    cy.get('[data-cy="withdraw-amount-input"]').should('be.visible');
    cy.get('[data-cy="withdraw-submit"]').should('contain.text', 'Withdraw');
  });

  it('switches to the Transfer tab', () => {
    cy.get('[data-cy="transfer-tab"]').click();
    cy.get('[data-cy="transfer-account-select"]').should('be.visible');
    cy.get('[data-cy="transfer-receiver-input"]').should('be.visible');
    cy.get('[data-cy="transfer-amount-input"]').should('be.visible');
    cy.get('[data-cy="transfer-submit"]').should('contain.text', 'Transfer');
  });

  it('deposits successfully and shows a success message', () => {
    cy.intercept('POST', '**/api/accounts/1/transactions/deposit', {
      statusCode: 200,
      body: {
        id: 10,
        type: 'DEPOSIT',
        amount: 250,
        status: 'COMPLETED',
        description: 'Test deposit',
      },
    }).as('deposit');

    cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' });

    cy.get('[data-cy="deposit-account-select"]').select('1');
    cy.get('[data-cy="deposit-amount-input"]').type('250');
    cy.get('[data-cy="deposit-submit"]').click();

    cy.wait('@deposit');
    cy.get('[data-cy="success-message"]').should('contain.text', '250');
  });

  it('shows an error message when deposit fails', () => {
    cy.intercept('POST', '**/api/accounts/1/transactions/deposit', {
      statusCode: 422,
      body: { error: 'Account is not active' },
    }).as('failedDeposit');

    cy.get('[data-cy="deposit-account-select"]').select('1');
    cy.get('[data-cy="deposit-amount-input"]').type('100');
    cy.get('[data-cy="deposit-submit"]').click();

    cy.wait('@failedDeposit');
    cy.get('[data-cy="error-message"]').should('be.visible');
  });

  it('withdraws successfully and shows a success message', () => {
    cy.intercept('POST', '**/api/accounts/1/transactions/withdraw', {
      statusCode: 200,
      body: {
        id: 11,
        type: 'WITHDRAWAL',
        amount: 100,
        status: 'COMPLETED',
        description: 'ATM',
      },
    }).as('withdraw');

    cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' });

    cy.get('[data-cy="withdraw-tab"]').click();
    cy.get('[data-cy="withdraw-account-select"]').select('1');
    cy.get('[data-cy="withdraw-amount-input"]').type('100');
    cy.get('[data-cy="withdraw-submit"]').click();

    cy.wait('@withdraw');
    cy.get('[data-cy="success-message"]').should('contain.text', '100');
  });

  it('shows an error when withdrawing more than the balance', () => {
    cy.intercept('POST', '**/api/accounts/1/transactions/withdraw', {
      statusCode: 422,
      body: { error: 'Insufficient balance' },
    }).as('failedWithdraw');

    cy.get('[data-cy="withdraw-tab"]').click();
    cy.get('[data-cy="withdraw-account-select"]').select('1');
    cy.get('[data-cy="withdraw-amount-input"]').type('99999');
    cy.get('[data-cy="withdraw-submit"]').click();

    cy.wait('@failedWithdraw');
    cy.get('[data-cy="error-message"]').should('contain.text', 'Insufficient balance');
  });

  it('transfers successfully and shows a success message', () => {
    cy.intercept('POST', '**/api/accounts/1/transactions/transfer', {
      statusCode: 200,
      body: {
        id: 12,
        type: 'TRANSFER',
        amount: 200,
        status: 'COMPLETED',
        description: 'Rent',
      },
    }).as('transfer');

    cy.intercept('GET', '**/api/accounts', { fixture: 'accounts.json' });

    cy.get('[data-cy="transfer-tab"]').click();
    cy.get('[data-cy="transfer-account-select"]').select('1');
    cy.get('[data-cy="transfer-receiver-input"]').type('1234567890ABCDEF');
    cy.get('[data-cy="transfer-amount-input"]').type('200');
    cy.get('[data-cy="transfer-submit"]').click();

    cy.wait('@transfer');
    cy.get('[data-cy="success-message"]').should('contain.text', '200');
  });

  it('shows an error when the receiver account is not found', () => {
    cy.intercept('POST', '**/api/accounts/1/transactions/transfer', {
      statusCode: 404,
      body: { error: 'Receiver account not found' },
    }).as('failedTransfer');

    cy.get('[data-cy="transfer-tab"]').click();
    cy.get('[data-cy="transfer-account-select"]').select('1');
    cy.get('[data-cy="transfer-receiver-input"]').type('AAAAAAAAAAAAAAAA');
    cy.get('[data-cy="transfer-amount-input"]').type('50');
    cy.get('[data-cy="transfer-submit"]').click();

    cy.wait('@failedTransfer');
    cy.get('[data-cy="error-message"]').should('contain.text', 'Receiver account not found');
  });
});
