# CampusConnect Web

A responsive, static website companion to the CampusConnect Android app. It uses the same dark violet design language, works as a deployable static site, and can connect to the existing CampusConnect API.

## Run locally

From this directory, start any static file server. For example:

```powershell
py -m http.server 8080
```

Open <http://localhost:8080>. Without a configured API, the site opens in preview mode with sample campus posts and events. Creating posts in preview mode only changes the current page.

## Connect the API

Set `API_BASE_URL` near the top of `app.js` to the deployed API base, including `/api/v1`, before deploying. For example:

```js
const CONFIG = { API_BASE_URL: window.CAMPUSCONNECT_API_BASE_URL || localStorage.getItem('cc-api-url') || 'https://api.example.edu/api/v1' };
```

The API must allow the website origin in its `CORS_ORIGINS` setting. For local development, include `http://localhost:8080`. The client uses the existing `/auth`, `/posts`, and `/events` endpoints. It stores access and refresh tokens in browser local storage; use HTTPS for deployed sites. Registration and sign-in require the API, and the demo feed is shown when it is unavailable.

## Deploy

Upload the contents of this `web/` folder to a static hosting service such as Cloudflare Pages, Netlify, or GitHub Pages. Set the API base URL in `app.js` and allow the final site origin in the backend CORS configuration. This folder has no build step or npm dependencies. The repository itself remains private; hosting a site is a separate decision from changing GitHub repository visibility.

## Current scope

- Responsive campus feed with search and feed filters.
- Post creation, reactions, saved posts, and event previews.
- API backed sign in, registration, feed, event list, post creation, and reactions.
- Friendly sample preview when opened without an API connection.
- Mobile navigation and accessible dialogs.

Stories, direct messages, circle membership, comments, image uploads, and profile editing remain preview interactions until their complete web workflows are connected.
