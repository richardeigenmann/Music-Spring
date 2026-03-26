/// <reference types="cypress" />

console.log('DEBUG: Commands.ts has been loaded!');

// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
//
// declare global {
//   namespace Cypress {
//     interface Chainable {
//       login(email: string, password: string): Chainable<void>
//       drag(subject: string, options?: Partial<TypeOptions>): Chainable<Element>
//       dismiss(subject: string, options?: Partial<TypeOptions>): Chainable<Element>
//       visit(originalFn: CommandOriginalFn, url: string, options: Partial<VisitOptions>): Chainable<Element>
//     }
//   }
// }

Cypress.Commands.add('verifyDevEnvironment', () => {
  cy.log('--- STARTING SAFETY CHECK ---');
  return cy
    .visit('/status')
    .contains('Backend URL:')
    .next()
    .invoke('text')
    .then((text) => {
      const cleanText = text.trim();
      expect(cleanText).to.contain('8002');
      cy.log(`Verified Backend: ${cleanText}`);
    });
});

Cypress.Commands.add('verifyHomepageShowsUp', () => {
  cy.visit('/');

  // Target the specific images directly and check visibility + load status in one go
  const assets = ['music_logo.gif', 'music_title.gif'];

  assets.forEach((asset) => {
    cy.get(`img[src*="assets/${asset}"]`)
      .should('be.visible')
      .and(($img) => {
        // "naturalWidth" check confirms the image isn't a 404/broken icon
        expect(($img[0] as HTMLImageElement).naturalWidth).to.be.greaterThan(0);
      });
  });
});
