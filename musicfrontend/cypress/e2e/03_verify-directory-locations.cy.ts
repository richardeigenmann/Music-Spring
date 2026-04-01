describe('Search for a track', () => {
  before(() => {
    cy.verifyDevEnvironment();
  });

  beforeEach(() => {
    cy.visit('/');
  });

  it('Should find the track location for a track in the root mp3 directory', () => {
    cy.searchForTrack('subterranean-pulse').click();

    cy.url().should('include', '/track');

    cy.get('input.file-location-input').should('have.value', '/');
  });

  it('Should find a track location for a track in the /1/ directory', () => {
    cy.searchForTrack('uplifting-journey').click();

    cy.url().should('include', '/track');

    cy.get('input.file-location-input').should('have.value', '/1/');
  });

});
