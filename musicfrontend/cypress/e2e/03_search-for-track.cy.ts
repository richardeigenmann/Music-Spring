describe('Search for a track', () => {
  before(() => {
    cy.verifyDevEnvironment();
  });

  it('Should find a track from the recently importes test files', () => {
    cy.searchForTrack('subterranean-pulse');
  });
});
