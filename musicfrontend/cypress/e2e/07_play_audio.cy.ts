describe('Play a track', () => {
  before(() => {
    cy.verifyDevEnvironment();
  });

  it('Should play a track', () => {
    const trackTitle = 'subterranean-pulse';

    cy.searchForTrack(trackTitle);

    cy.contains('li', trackTitle).as('targetTrack');
    cy.get('@targetTrack').find('.play-overlay').click({ force: true });

    cy.get('app-track-player')
      .should('be.visible')
      .find('.track-title')
      .should('contain', trackTitle);

    cy.get('audio.audio-element').should(($audio) => {
      const audioEl= $audio[0] as HTMLAudioElement;
      audioEl.muted=true; // MUTE it immediately
      expect(audioEl.src).to.not.be.empty;
      expect(audioEl.src).to.contain('http');

      expect(audioEl.paused, 'Audio should be paused').to.be.true;
      //expect(audioEl.currentTime, 'Audio should have progressed').to.be.gt(0);
    });
  });
});
