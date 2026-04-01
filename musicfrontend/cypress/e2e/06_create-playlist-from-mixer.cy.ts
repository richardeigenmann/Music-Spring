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

    cy.contains('.cdk-drag', 'House Music').cdkDragTo('#canList');
    cy.get('#canList .cdk-drag').should('have.length', 1).and('contain', 'House Music');

    cy.wait(500);

    cy.contains('.cdk-drag', 'Trance').cdkDragTo('#canList');
    cy.get('#canList .cdk-drag').should('have.length', 2).and('contain', 'Trance');

    const playlistName = 'My Cool Playlist';
    cy.get('input[placeholder="New Playlist Name"]')
      .should('be.visible')
      .clear()
      .type(playlistName);

    cy.contains('button', 'Save Mix').should('be.visible').click();

    cy.get('input.search-input').should('be.visible').clear().type(`${playlistName}{enter}`);
    cy.url().should('include', '/search');
    cy.get('app-track-list').find('span.track-title').should('be.visible').and('contain', 'subterranean-pulse').and('contain', 'sunset-haze-groove');
  });


});
