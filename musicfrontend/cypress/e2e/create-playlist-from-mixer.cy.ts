import '@4tw/cypress-drag-drop';

describe('Create a playlist in the mixing board', () => {
  before(() => {
    // Run the asset check to ensure the UI is rendered correctly
    cy.verifyHomepageShowsUp();
    // Run the shared safety check before doing anything else
    cy.verifyDevEnvironment();
    // cy.resetMusicDatabase();
  });

  it('Should create a playlist in the mixing board', () => {
    cy.contains('button.musicdb-button', '☰ Menu').click();
    cy.contains('button', 'Mixing Board').should('be.visible').click();

    cy.contains('.cdk-drag', 'Acid House').cdkDragTo('#canList');
    cy.get('#canList', { timeout: 2000 }).should('contain', 'Acid House'); //cy.contains('div.cdk-drag', 'Blues').drag('#canList', { force: true });
  });

  //cy.url({ timeout: 10000 }).should('include', '/unclassified');
});
