declare global {
  namespace Cypress {
    interface Chainable {
      verifyDevEnvironment(): Chainable<void>;
      verifyHomepageShowsUp(): Chainable<void>;
    }
  }
}
