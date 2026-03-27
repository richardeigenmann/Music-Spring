describe('Verify the OpenAPI spec', () => {
  it('should match the OpenAPI spec', () => {
    // 1. Manually fetch the schema JSON first
    cy.request('GET', 'http://localhost:8002/v3/api-docs').then((schemaResponse) => {
      const mySchema = schemaResponse.body; // This is now a Javascript Object

      // 2. Run the API test AND chain the validator directly to it
      // Notice there is no .then() here!
      cy.request('GET', 'http://localhost:8002/api/version')
        .validateSchema(mySchema, {
          endpoint: '/api/version',
          method: 'GET',
          status: 200,
        });
    });
  });
});
