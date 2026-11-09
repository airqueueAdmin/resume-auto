# Google Analytics 4 integration verification

## Scope

Add optional GA4 page-view measurement to the React single-page application without sending career
content, account identifiers, or URL query parameters. Keep analytics disabled until both a valid
measurement ID and explicit user consent are present.

## Design

- `GA_MEASUREMENT_ID` is injected into the Nginx-served `runtime-config.js`; it is configuration, not a
  secret. Local Vite development can use `VITE_GA_MEASUREMENT_ID`.
- The Google tag is not requested before the user chooses **허용**.
- React route changes emit manual `page_view` events. Only `location.pathname` is used, preventing email
  and return-path query parameters from entering analytics.
- Advertising storage, ad user data, ad personalization, Google signals, and ad-personalization signals
  are disabled.
- The consent decision is stored locally. The privacy page shows the current state and supports later
  grant or withdrawal; withdrawal disables subsequent tracking and removes accessible `_ga*` cookies.
- A missing or malformed measurement ID leaves analytics disabled without blocking the application.

## Files

- `frontend/src/analytics.ts`: consent-aware Google tag loader and SPA page-view tracking
- `frontend/src/App.tsx`: consent UI, route tracking, and privacy disclosure
- `frontend/public/runtime-config.js`: development-safe empty runtime configuration
- `frontend/docker-entrypoint.sh`: validated runtime measurement-ID injection
- `docker-compose.yml` and `render.yaml`: deployment environment contract

## Verification

- `npm run build`: passed; existing React Router directive and bundle-size warnings remain.
- `docker compose config --quiet`: passed.
- `docker compose build frontend`: passed.
- `AiCareerPlatformApplicationTests#authenticationSupportsRefreshRotationLogoutAndOwnership`: passed
  with the updated privacy-policy version.

## Activation and manual verification

1. Create or select a GA4 web data stream and copy its `G-...` measurement ID.
2. Set `GA_MEASUREMENT_ID` on the frontend service and redeploy it.
3. Before consent, confirm that the browser does not request `googletagmanager.com/gtag/js`.
4. Choose **허용**, navigate between application sections, and verify one `page_view` per pathname in
   GA4 DebugView or Tag Assistant.
5. Open `/privacy`, choose **분석 수집 철회**, and verify that later route changes send no page views.

## Known limitation

The repository cannot activate a real GA4 destination without the deployment's measurement ID. No
custom business events or user-ID association are included in this step.
