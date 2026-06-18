# StreetRise Android App Setup Guide (Capacitor & Native Shell Wrapper)

This guide documents how the StreetRise React/Vite/Tailwind web application (`app.streetrise.org`) is converted into a production-ready Android App. It details the setup steps, environment configurations, build procedures, and custom native enhancements implemented in Jetpack Compose to deliver a premium user experience.

---

## 🚀 Overview of What Was Built & Installed

To deliver maximum speed, stability, and compatibility without rewriting the live product, we configured a hybrid architecture. It integrates a **Capacitor Configuration layer** for frontend web compilation alongside an **advanced Native Jetpack Compose Shell Wrapper** that executes directly in the Android build toolchain.

1. **Capacitor Integration**: Installed and configured `@capacitor/core`, `@capacitor/cli`, and `@capacitor/android` configuration scripts inside the root folder (`package.json`, `capacitor.config.json`).
2. **Native Jetpack Compose Hybrid Shell**: Rewrite of `MainActivity.kt` with modern Material 3 overlays.
3. **Dynamic Geolocation Permission Dialog**: Displays a beautiful Material 3 rationale explanation before calling the native Android Geolocation prompt, ensuring smooth, graceful accessibility.
4. **Intelligent Deep-Link Routing**:
   - Maintains all internal routing (authentication redirect, resource pages, booking requests, providers, dashboards) inside the secure in-app WebView.
   - Intercepts and activates the **Android Dialer** for standard `tel:` links.
   - Launches native **Google Maps** or preferred mapping clients for `geo:` URLs and direction paths.
   - Launches external web links (like privacy policies, partners, or Stripe) safely inside the **system web browser** and returns the user to the app cleanly.
5. **Robust Browser Memory**: Activated `domStorageEnabled`, `javaScriptEnabled`, `databaseEnabled`, and synchronized full cookie session persistence to guarantee that **Supabase Auth and Realtime subscriptions** remain logged in across apps restarts.
6. **Full Native & Capacitor Push Notifications (FCM)**:
   - Integrated Google Firebase Cloud Messaging (FCM) library managed dynamically with a programmatic builder to support zero-crash local compiling without raw `google-services.json` requirements.
   - Built a background `StreetRiseMessagingService` with self-registering high-priority Android O notification channels (`streetrise_alerts`).
   - Designed a Capacitor Push Notification plugin bridge that seamlessly binds standard web calls (e.g. `PushNotifications.register()` and `PushNotifications.addListener()`) to the native Android client so unmodified hybrid web logic works natively.
   - Created an interactive Material 3 diagnostics card inside the "About" tab allowing admins and test coordinators to inspect state and copy the unique registration token.

---

## 🔐 Environment Variables

The StreetRise client app uses **only public-safe Supabase variables**. No service role keys or private database passwords are built or bundled inside the client-side packages.

Copy the `.env.example` file to create your `.env`:
```bash
# Public Supabase Client URL
VITE_SUPABASE_URL=https://your-project.supabase.co

# Public Supabase Anon Client Key (Safe for client-side bundling)
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

*Note: The Android WebView loads the Cloudflare Pages web build which already manages these variables on the server. Your native Android application doesn't bundle hardcoded credentials, making it secure and resilient to key leaks.*

---

## 🛠️ Step-by-Step Developer Operations Workflow

### 1. Build the Web App
If you are developing the React/Vite site locally, build the production web package:
```bash
npm install
npm run build
```
This compiles your assets into the designated `dist/` directory.

### 2. Configure & Sync Capacitor
To link the web build to the Android compiler:
```bash
# Sync the built web assets into the Capacitor native build context
npm run android:sync
```

### 3. Build & Run the Android App in AI Studio or Android Studio
To compile or test the application within your target environments:
- **Build immediately in standard terminal**:
  ```bash
  gradle assembleDebug
  ```
- **Sync using capacitor CLI**:
  ```bash
  npx cap sync
  ```
- **Open in Android Studio**:
  Launch Android Studio and select the root directory (or the nested `app` workspace). It will automatically sync Gradle and enable editing.

---

## 📲 How to Run and Test

### Running on a Virtual Device / Emulator
1. Open Android Studio -> Device Manager.
2. Launch your preferred Virtual Device (e.g., Pixel 7 running Android API 34).
3. Click the **Run** button (green play icon) or execute:
   ```bash
   gradle installDebug
   ```

### Running on a Physical Android Device
1. On your Android phone, go to **Settings > About Phone** and tap **Build Number** 7 times to enable Developer Options.
2. Enable **USB Debugging** in Developer Options.
3. Connect the phone to your computer via USB. On the phone, accept the "Allow USB Debugging" permission prompt.
4. Android Studio will display your device in the toolbar. Click **Run**.

---

## 📦 Preparing for Production Release (Google Play Store)

When preparing to publish StreetRise to the Google Play Store:

1. **Generate a Signing Keystore (JKS)**:
   ```bash
   keytool -genkey -v -keystore streetrise-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Configure production variables**: Save your keystore safely. Define your production signing credentials in your environment variables/CI pipeline:
   - `KEYSTORE_PATH`
   - `STORE_PASSWORD`
   - `KEY_PASSWORD`
3. **Generate Release Bundle (AAB)**:
   In Android Studio, go to **Build > Generate Signed Bundle / APK**, choose **Android App Bundle**, select your signed Keystore, and build. This produces your `.aab` file located in `app/release/`.

---

## 💳 Stripe Checkout & Donation flows

The donation flow requires safe payment handling. Currently, the donation screen uses clean placeholders to avoid mock transactions.

For a live Stripe flow:
1. **Redirect Integration**: Standard Stripe Checkout session URLs are generated by your CloudflarePages/Supabase backend server.
2. **System Browser Launch**: The native Android wrapper intercepts the Stripe checkout URL and launches the secure system browser natively (already enabled in `MainActivity.kt`). This protects user banking details.
3. **Clean Return**: Use Stripe's success/cancel redirection URLs pointing back to deep links with custom schemes (e.g. `streetrise://donate-success`). Ensure you register this custom intent filter in your `AndroidManifest.xml` so the user is returned back to the app smoothly.

---

## 📋 Comprehensive Testing Checklist

Before submitting to staging or production, verify all criteria in this checklist:

### 📱 Launch & Core State
- [ ] App launches successfully without crashing on Android Emulator.
- [ ] App launches successfully without crashing on a physical device.
- [ ] Home Page loads and is responsive.
- [ ] User Interface respects safe areas, status bars, and navigation pills.
- [ ] App can survive being backgrounded and resumed without losing state.
- [ ] App can be refreshed with a pull-down gesture or action.
- [ ] No private or server-role developer keys are bundled.

### 🗺️ Map-Based Discovery
- [ ] Interactive OpenStreetMap/Leaflet map renders correctly inside the Android shell.
- [ ] Zoom, pan, and scroll gestures work smoothly.
- [ ] Resource markers represent shelter, food, and hygiene services accurately.
- [ ] Location filtering categories (e.g., shelter, day-use) function instantly.

### 📍 Permissions & Core Services
- [ ] Geolocation rationale explanation dialog triggers gracefully before the system prompt.
- [ ] Map successfully centers on user location after enabling permission.
- [ ] App handles permission denial gracefully (displaying clear guidance but not crashing).
- [ ] Back button goes back step-by-step inside the website history rather than immediately exiting the app.

### 🌐 Outbound Intents
- [ ] Clicking a phone link launches the Android system dialer (`tel:` protocol).
- [ ] Clicking details direction triggers native directions in Google Maps, Apple Maps, or system favorite routing tools.
- [ ] External links (partners, informational sheets) launch the Google Chrome / system browser.
- [ ] In-app paths (booking forms, checklists, FAQs) preserve state and stay inside the app.

### 🧑‍💻 Provider & Admin Control
- [ ] Provider Onboarding and provider login flows operate correctly.
- [ ] Provider Dashboard displays lists and manages bookings.
- [ ] Supabase Realtime channel updates vacant beds and reservation updates instantly.
- [ ] Admin Portal loads moderation streams, review boards, and setting configurations safely.

---

## ⚠️ Known Limitations & Future Roadmap (TODOs)

- **Local Asset Packaging**: For offline operation, cache static CSS, Web Fonts, and SVG resources directly in the assets directory to reduce network requests on slower edge connections.
- **Biometric Login**: Implement a native `@capacitor/fingerprint-layout` or standard Biometrics UI to allow providers and administrators to sign in safely using fingerprint/face sensors.

