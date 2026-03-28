import { defineConfig } from 'cypress';

export default defineConfig({
  allowCypressEnv: true,

  e2e: {
    baseUrl: 'http://localhost:4200',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on, config) {
      on('before:browser:launch', (browser, launchOptions) => {
      });
    },
  },
});
