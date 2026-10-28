# CampusConnect Web

A responsive, static website companion to the CampusConnect Android app. It uses the same dark violet design language, works as a deployable static site, and can connect to the existing CampusConnect API.

## Run locally

From this directory, start the included preview server:

```powershell
node server.js
```

Open <http://localhost:8080>. Alternatively, use `python -m http.server 8080`. Without a configured API, the site opens in preview mode with sample campus posts and events. Creating posts in preview mode only changes the current page.

## Connect the API

Set `API_BASE_URL` near the top of `app.js` to the deployed API base, including `/api/v1`, before deploying. For example:

```js
const CONFIG = { API_BASE_URL: window.CAMPUSCONNECT_API_BASE_URL || localStorage.getItem('cc-api-url') || 'https://api.example.edu/api/v1' };
```

The API must allow the website origin in its `CORS_ORIGINS` setting. For local development, include `http://localhost:8080`. The client uses the existing `/auth`, `/posts`, and `/events` endpoints. It stores access and refresh tokens in browser local storage; use HTTPS for deployed sites. Registration and sign-in require the API, and the demo feed is shown when it is unavailable.

## Deploy

This folder includes a Render static-site blueprint at [`render.yaml`](render.yaml). When creating a Render Blueprint for this repository, select `web/render.yaml` as the Blueprint file. The repository-root `render.yaml` describes the API, database, and cache, including paid services; do not select it when you only want to host the website.

The static site serves in preview mode until an API URL is configured. Set the API base URL in `app.js` and allow the final site origin in the backend `CORS_ORIGINS` configuration. A deployed static website is internet-accessible even while its GitHub repository stays private. This folder has no npm dependencies.

## Current scope

- Responsive campus feed with search and feed filters.
- Post creation, reactions, saved posts, and event previews.
- API backed sign in, registration, feed, event list, post creation, and reactions.
- Friendly sample preview when opened without an API connection.
- Mobile navigation and accessible dialogs.

Stories, direct messages, circle membership, comments, image uploads, and profile editing remain preview interactions until their complete web workflows are connected.
