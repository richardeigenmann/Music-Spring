describe('Start a Scan of the mp3 directory', () => {

before(() => {
    cy.verifyHomepageShowsUp();
    cy.verifyDevEnvironment();
    cy.resetMusicDatabase();
  });

  it('Should open the Hamburger Menu and should click on the Scan directories button', () => {
    cy.contains('button.musicdb-button', '☰ Menu').click();

    cy.contains('button', 'Scan mp3 directory')
      .should('be.visible')
      .click();

    cy.get('.scan-progress-box', { timeout: 2500 }).should('be.visible');

    cy.url({ timeout: 10000 }).should('include', '/unclassified');

  });

});
