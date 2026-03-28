import './commands';
declare global {
  namespace Cypress {
    interface Chainable {
      verifyDevEnvironment(): Chainable<void>;
      verifyHomepageShowsUp(): Chainable<void>;
      resetMusicDatabase(): Chainable<void>;
      cdkDragTo(targetSelector: string): Chainable<void>;
      searchForTrack(searchText: string): Chainable<Element>;
      classifyTrack(searchText: string, classificationType: string, classification: string): Chainable<Element>;
    }
  }
}
