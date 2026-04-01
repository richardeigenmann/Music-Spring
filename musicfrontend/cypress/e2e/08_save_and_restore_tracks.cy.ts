describe('Save and Restore Tracks', () => {
  before(() => {
    cy.verifyDevEnvironment();
  });

  it('should save and restore track data', () => {
    // 1. Get the initial count and store it
    cy.getTotalTrackCount().then((initialCount) => {
      expect(initialCount).to.be.greaterThan(0);

      // 2. Dump the actual data
      cy.request('GET', 'http://localhost:8002/api/tracks/dump').then((response) => {
        expect(response.status).to.eq(200);
        const backedUpData = response.body;
        cy.log('Database dumped successfully');

        // 3. Reset the database
        cy.resetMusicDatabase();

        // 4. Verify it is empty
        cy.getTotalTrackCount().should('eq', 0);

        // 5. Reload the data (Passing the backedUpData from the closure above)
        const formData = new FormData();

        // We need to convert the JSON object into a Blob so the
        // multipart request handles it as a "file" or "part"
        const blob = new Blob([JSON.stringify(backedUpData)], { type: 'application/json' });

        // 'file' is the most common key name for Spring multipart,
        // but check if your API expects a different key like 'tracks'
        formData.append('file', blob);

        cy.request({
          method: 'POST',
          url: 'http://localhost:8002/api/tracks/bulk',
          body: formData,
          // Note: Do NOT manually set Content-Type header;
          // Cypress/the browser needs to set the 'boundary' string automatically
        }).then((response) => {
          expect(response.status).to.be.oneOf([200, 201]);
          cy.log('Database reloaded via Multipart');
        });

        // 6. Final verification: Does current count match the initial count?
        cy.getTotalTrackCount().should('eq', initialCount);
      });
    });
  });
});
