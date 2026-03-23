import { browser } from 'k6/browser';
import { check } from 'k6';

export const options = {
  scenarios: {
    ui: {
      executor: 'constant-vus',
      vus: 1, // Start small! 1000 will crash your machine.
      duration: '5s',
      options: {
        browser: {
          type: 'chromium',
          args: [
            'no-sandbox',
            'disable-setuid-sandbox',
            'disable-dev-shm-usage', // Uses /tmp instead of /dev/shm
            'disable-gpu',           // Essential for Docker/Headless
            'no-zygote',             // Disables the process forking model
            'single-process'         // Can help in restricted environments
          ],
        },
      },
    },
  },
};

export default async function () {
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // 1. Go to the Angular SPA
    //await page.goto('http://localhost:4200/groups/92');
    await page.goto('http://localhost:4200/groups/92', { waitUntil: 'networkidle' });

    // 2. Wait for a specific element (like your music title) to ensure Angular has loaded
    //const title = page.locator('h1'); // Adjust this selector to match your app
    //await title.waitFor();

    // 3. Verify the page loaded
    //check(page, {
    //  'header is visible': await title.isVisible(),
    //});

    // Optional: Take a screenshot to see what k6 is seeing
    await page.screenshot({ path: 'screenshot.png' });

  } finally {
    await page.close();
  }
}
