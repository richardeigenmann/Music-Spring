describe('Start a Scan of the mp3 directory', () => {

before(() => {
    // Run the asset check to ensure the UI is rendered correctly
    cy.verifyHomepageShowsUp();
    // Run the shared safety check before doing anything else
    cy.verifyDevEnvironment();
  });

  it('Should open the Hamburger Menu and should click on the Scan directories button', () => {
    //cy.visit('/');

    cy.get('.musicdb-button').click();

    cy.contains('button', 'Scan mp3 directory')
      .should('be.visible')
      .click();
  });

});
