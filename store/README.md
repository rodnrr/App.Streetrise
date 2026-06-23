# StreetRise — Store Publishing Content

This folder contains ready-to-paste content for publishing **StreetRise**
(`org.streetrise.app` / `app.streetrise.org`) to app stores, plus the
compliance forms each store requires.

| File | Purpose |
|------|---------|
| [`google-play-listing.md`](./google-play-listing.md) | Google Play Console store listing copy (title, descriptions, etc.) |
| [`apple-app-store-listing.md`](./apple-app-store-listing.md) | App Store Connect listing copy |
| [`data-safety.md`](./data-safety.md) | Google Play **Data safety** form + Apple **Privacy Nutrition Labels** answers |
| [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md) | Privacy policy to host at a public URL (required by both stores) |
| [`assets-checklist.md`](./assets-checklist.md) | Graphic assets each store requires (sizes/specs) |

## ⚠️ Before you submit — fill in these placeholders

Search all files for `[[ ... ]]` and replace with your real values:

- `[[LEGAL_ENTITY]]` — the legal name that owns the app (org/company/individual).
- `[[CONTACT_EMAIL]]` — public support/privacy contact email.
- `[[PRIVACY_POLICY_URL]]` — the public URL where `PRIVACY_POLICY.md` is hosted
  (e.g. `https://app.streetrise.org/privacy`).
- `[[WEBSITE_URL]]` — marketing/site URL (e.g. `https://streetrise.org`).
- `[[MAILING_ADDRESS]]` — postal address (some jurisdictions require it in the policy).
- `[[EFFECTIVE_DATE]]` — date the policy takes effect.

## Notes on accuracy

The Data Safety / privacy content was written from what the app **currently**
does in this repo:

- Loads `app.streetrise.org` in a WebView (Supabase auth + realtime).
- Requests **device location** to find nearby resources and center the map.
- Requests **notification** permission and registers a **push token** (FCM).
- Ships an offline **resource seed** (`resources_seed.json`) — public data, no PII.
- Donations / payments are handled by **Stripe in the system browser** (StreetRise
  does not collect card data in-app).

If you add analytics, crash reporting, ads, or in-app payments later, you **must**
update `data-safety.md` and `PRIVACY_POLICY.md` — both stores treat inaccurate
disclosures as a policy violation.
