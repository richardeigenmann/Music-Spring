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
    .contains('strong', 'Database URL:')
    .parent()
    .then(($el) => {
      const fullText = $el.text();
      expect(fullText).to.contain('jdbc:h2:mem:musicdb');
      cy.log(`Verified Backend is connected to H2 database: ${fullText}`);
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

Cypress.Commands.add('resetMusicDatabase', () => {
  cy.log(`Hitting REST endpoint /api/clear-db to reset the database`);
  // 1. Hit the clear database endpoint
  // Note: Check your Swagger 'Try it out' to see if this is a POST or DELETE
  cy.request('POST', 'http://localhost:8002/api/clear-db').then((response) => {
    expect(response.status).to.eq(200);
  });

  // 2. Verify the count is now 0
  cy.request('GET', 'http://localhost:8002/api/version').then((response) => {
    expect(response.body).to.have.property('totalTrackCount', 0);
  });
});

// I struggled with this a bit. Angular needs to see the mouse move at least 5 pixles on
// the source object to trigger the drag. Likewise simply teleporting the mouse to the target
// and releasing it doesn't get it to detect the "enter" into the drop event. So a 3 pixel move
// helps trigger that.
Cypress.Commands.add('cdkDragTo', { prevSubject: 'element' }, (subject, targetSelector) => {
  cy.wrap(subject)
    .realMouseDown({ position: 'center' })
    .realMouseMove(10, 0, { position: 'center' }); // The "Unlock" move

  cy.get(targetSelector)
    .realMouseMove(10, 0, { position: 'center' }) // The "Entry" move
    .realMouseUp({ position: 'center' });
});


// types the searchText into the search box and ensures that the resulting tracks contain the searchText
Cypress.Commands.add('searchForTrack', (searchText) => {
    cy.get('input.search-input')
      .should('be.visible')
      .clear()
      .type(`${searchText}{enter}`);

    cy.url().should('include', '/search');

    cy.get('app-track-list') // 1. Find the parent component
      .find('span.track-title') // 2. Look inside it for the span
      .should('be.visible') // 3. Assert it is there and visible
      .and('contain', searchText); // 4. Check the text
});


Cypress.Commands.add('classifyTrack', (searchText, classificationType, classification) => {
    cy.searchForTrack(searchText);

    cy.get('app-track-list')
      .find('span.track-title')
      .should('be.visible')
      .and('contain', searchText)
      .click();

    cy.url()
      .should('include', '/track')
      .then((url) => {
        const trackId = url.split('/').pop();
        cy.contains('span.tag-label', classification).find('button.btn-icon').click();

        cy.contains('button', 'Save Changes').click();

        // check in the api that the classification was saved correctly
        cy.request({
          method: 'GET',
          url: `http://localhost:8002/api/track/${trackId}`,
          headers: {
            accept: 'application/json',
          },
        }).then((response) => {
          expect(response.status).to.eq(200);

          const genreData = response.body[classificationType];
          expect(genreData).to.include(classification);
        });
      });
});
