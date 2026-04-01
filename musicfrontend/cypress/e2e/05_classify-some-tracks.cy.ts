describe('Classify some tracks', () => {
  before(() => {
    cy.verifyDevEnvironment();
  });

  it('Classify some tracks', () => {
    cy.classifyTrack('subterranean-pulse', 'Genre', 'House Music');
    cy.classifyTrack('sunset-haze-groove', 'Genre', 'Trance');
  });
});
