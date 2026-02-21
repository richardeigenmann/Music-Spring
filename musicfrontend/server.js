const express = require('express');
const path = require('path');
const app = express();

// The Backend URL will be read from the environment
// We remove any trailing slash to avoid double slashes in API calls
const BACKEND_URL = (process.env.BACKEND_URL || 'http://localhost:8002').replace(/\/$/, '');
const PORT = process.env.PORT || 4200;

// Serve the static files from the Angular build
// Note: Angular 17+ defaults to dist/musicfrontend/browser
app.use(express.static(path.join(__dirname, 'dist/musicfrontend/browser')));

// Endpoint for the frontend to get its configuration
app.get('/config', (req, res) => {
  res.json({ apiUrl: BACKEND_URL });
});

// For all other routes, serve the index.html (SPA routing)
// Using index.html as the final handler to avoid path-to-regexp complexities in Express 5
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'dist/musicfrontend/browser/index.html'));
});

app.listen(PORT, () => {
  console.log(`Frontend server running on port ${PORT}`);
  console.log(`Configured Backend URL: ${BACKEND_URL}`);
});
