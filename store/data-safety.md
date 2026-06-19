# Data Safety & Privacy Labels — StreetRise

This file gives you the exact answers for:

1. **Google Play → Data safety** form
2. **Apple App Store Connect → App Privacy** (nutrition labels)

> ⚠️ Both stores require these answers to match what the app **actually** does.
> The answers below reflect the current build (WebView of `app.streetrise.org`,
> device location, push notifications via FCM, no in-app payment capture, no
> third-party analytics/ads). **If you add analytics, crash reporting, ads, or
> in-app payments, you must update both this file and the consoles.**

---

## Part 1 — Google Play Data Safety form

### Overview answers

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data encrypted in transit? | **Yes** (HTTPS/TLS) |
| Do you provide a way for users to request that their data be deleted? | **Yes** — via `[[CONTACT_EMAIL]]` and account deletion (see Privacy Policy) |
| Has your data collection been independently reviewed/validated? | Optional — leave unchecked unless you have it |

### Data types — what to declare

For each item: **Collected = Yes**, **Shared** as noted, **Processed ephemerally**
where noted, **Required vs Optional** as noted. "Shared" means sent to a third
party; using a processor (like your own Supabase backend) is **collection**, not
"sharing."

#### Location
| Data type | Collected | Shared | Purpose(s) | Optional? |
|-----------|-----------|--------|------------|-----------|
| **Approximate location** | Yes | No | App functionality (show nearby resources, center map) | Optional (user grants permission) |
| **Precise location** | Yes | No | App functionality (find closest services, directions) | Optional (user grants permission) |

> Precise location is used only while using the app to find nearby resources; it
> is not used for advertising or tracking.

#### Personal info (only if users create accounts — e.g. providers/admins/users)
| Data type | Collected | Shared | Purpose(s) | Optional? |
|-----------|-----------|--------|------------|-----------|
| **Email address** | Yes | No | Account management, authentication | Required to create an account; not required to browse |
| **Name** | Yes (if collected at signup) | No | Account management | Optional |

#### App activity
| Data type | Collected | Shared | Purpose(s) | Optional? |
|-----------|-----------|--------|------------|-----------|
| **App interactions** (e.g. bookings/requests submitted by signed-in users) | Yes | No | App functionality (resource booking & provider coordination) | Optional |

#### Device or other identifiers
| Data type | Collected | Shared | Purpose(s) | Optional? |
|-----------|-----------|--------|------------|-----------|
| **Push notification token (FCM registration token)** | Yes | No | App functionality (deliver service/availability alerts) | Optional (user grants notification permission) |

> The FCM token is a device messaging identifier. Declare it under
> "Device or other IDs." It is used only to deliver notifications the user opted
> into — not for advertising or cross-app tracking.

### Data types you should NOT check (current build)
- ❌ Financial info / payment info — donations/payments go to **Stripe in the
  system browser**; StreetRise does not collect card or bank data in-app.
- ❌ Web browsing history, contacts, photos, audio, calendar, SMS.
- ❌ Analytics / crash logs — only if you have not added an SDK that collects them.
  (If you later add Firebase Analytics/Crashlytics, you must declare "Crash logs"
  and/or "Diagnostics," and likely "Sharing" with Google.)

### Security practices to check
- ✅ Data is encrypted in transit.
- ✅ Users can request data deletion.
- ✅ Committed to Play Families Policy: only if you target children (you do not).

---

## Part 2 — Apple App Privacy (Nutrition Labels)

In App Store Connect, for each item choose: **Data Used to Track You**,
**Data Linked to You**, or **Data Not Linked to You**. StreetRise does **not**
track users across apps/sites, so nothing goes under "Used to Track You."

### Location
| Data type | Linked to user? | Purpose | Used for tracking? |
|-----------|-----------------|---------|--------------------|
| **Precise Location** | Not Linked (if browsing anonymously) / Linked (if signed in) | App Functionality | No |
| **Coarse Location** | Same as above | App Functionality | No |

### Contact Info (only if accounts are used)
| Data type | Linked to user? | Purpose | Used for tracking? |
|-----------|-----------------|---------|--------------------|
| **Email Address** | Linked to You | App Functionality, Account Management | No |
| **Name** (if collected) | Linked to You | App Functionality | No |

### User Content / Identifiers
| Data type | Linked to user? | Purpose | Used for tracking? |
|-----------|-----------------|---------|--------------------|
| **Other User Content** (booking/requests) | Linked to You | App Functionality | No |
| **Device ID** (push token) | Not Linked / Linked | App Functionality (notifications) | No |

### Do NOT declare (current build)
- ❌ Purchases / Financial Info (handled externally by Stripe in-browser).
- ❌ Usage Data / Analytics — unless you add an analytics SDK.
- ❌ Browsing History, Search History, Contacts, Health, Sensitive Info.

### Tracking
- **Does this app use data to track you?** → **No.**
- No Identifier for Advertisers (IDFA), no ad networks, no cross-app tracking →
  you do **not** need an App Tracking Transparency (ATT) prompt.

---

## Required iOS `Info.plist` usage strings (so the OS prompts match these labels)

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>StreetRise uses your location to show shelters, food, and support services near you and to provide directions.</string>
```
Push notifications on iOS use the user-facing system prompt automatically when you
call `registerForRemoteNotifications` — no plist string is required, but you must
add the **Push Notifications** capability and an APNs key.
