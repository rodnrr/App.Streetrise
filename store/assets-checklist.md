# Store Graphic Assets Checklist — StreetRise

Text content is in the other files in this folder. These are the **image/graphic**
assets each store requires you to upload separately. The app launcher icon already
exists in `app/src/main/res/mipmap-*`.

## Google Play

| Asset | Spec | Required? |
|-------|------|-----------|
| App icon | 512 × 512 px, 32-bit PNG, ≤ 1 MB | ✅ Required |
| Feature graphic | 1024 × 500 px, PNG/JPG (no alpha) | ✅ Required |
| Phone screenshots | 2–8 images, 16:9 or 9:16, 320–3840 px per side | ✅ Required (min 2) |
| 7-inch tablet screenshots | up to 8 | Optional |
| 10-inch tablet screenshots | up to 8 | Optional |
| Promo / TV / Wear assets | various | Only if targeting those |

## Apple App Store

| Asset | Spec | Required? |
|-------|------|-----------|
| App icon | 1024 × 1024 px, PNG, no alpha/transparency, no rounded corners | ✅ Required |
| 6.7" iPhone screenshots | 1290 × 2796 px (or 2796 × 1290) | ✅ Required |
| 6.5" iPhone screenshots | 1242 × 2688 or 1284 × 2778 px | Required for older device support |
| 5.5" iPhone screenshots | 1242 × 2208 px | Optional (legacy) |
| iPad Pro 12.9" screenshots | 2048 × 2732 px | Required only if app supports iPad |
| App preview video | optional, per device size | Optional |

> Tip: Apple lets the largest iPhone size scale down to smaller sizes, so at
> minimum produce the **6.7"** set.

## Suggested screenshot set (works for both stores)

Capture these flows to show native value (important for Apple Guideline 4.2):

1. **Map view** centered on the user's location with resource markers.
2. **Category filter** active (e.g., shelters only).
3. **Resource detail** with address, hours, and the Call / Directions buttons.
4. **Notifications / alerts** screen.
5. **Provider dashboard** (if you want to highlight the coordination side).

Add a short caption overlay to each (e.g., "Find help near you," "One-tap
directions," "Real-time bed availability").
