# SecureHomes - Android Studio Native Project (Kotlin)
**Eduvos Module: Mobile Application and Web Services (ITMDA3 / ITMDA3-34)**
**Deliverable 4: Implementation (Group 14 - Midrand Campus)**

## Project overview
Native Android (Kotlin) app for the study *Usability vs Security Trade-off in Consumer IoT Smart
Home Configuration*. Participants add a simulated smart device and secure it using the version of
the setup screen they were assigned:

| Version | Screen |
|---|---|
| **A – Guided** (proposed design) | 4-step wizard: network pairing, access & 2FA, encryption & patching, review with live score |
| **B – Unguided** (baseline) | One technical form: hostname, static IPv4, subnet, gateway, admin password, cipher, 2FA / VLAN / UPnP / OTA flags |

- **A/B assignment:** each participant is assigned to Version A or B when they register (balanced
  random: the smaller group is chosen, ties at random) and always sees that version.
- **Fair comparison:** both versions start from the device's real factory state (default password,
  WPA2-TKIP, 2FA/VLAN/updates off) and are measured the same way: time on task, real validation
  error counts, abandonment (with the step where the user left) and the final security score.
- **Authentication:** registration and login with salted **PBKDF2-HMAC-SHA256** hashes (random salt
  per user), sessions that expire after 30 minutes, logout, and role checks on every screen
  (`BaseActivity`). Participants can only see and change their own devices.
- **Device management (CRUD):** add (radar scan), view and filter, reconfigure, power on/off,
  firmware update, delete.
- **Security scoring (`SecurityEvaluator.kt`):** password 25, encryption 25, 2FA 20, network
  isolation 15, automatic updates 15. 85+ = hardened. UPnP and outdated firmware are shown as
  advisories but not scored.
- **Survey (`SurveyActivity.kt`):** full 10-item System Usability Scale and 5-item raw NASA-TLX,
  nothing pre-selected, saved under the participant's real version.
- **Research analytics (`AnalyticsActivity.kt`, researcher only):** Version A vs B results
  **calculated live from the database** (attempts, time, errors, score, hardened rate, abandonment,
  SUS, NASA-TLX) plus the latest 50 telemetry events. Shows "no data yet" until participants take part.

## How to open and run
1. Open Android Studio → **File → Open** → select the `securehomes-android` folder.
2. Let Gradle sync, then **Build → Rebuild Project**.
3. Start an emulator (API 24+) and press **Run ▶**.
4. Unit tests: right-click `app/src/test/java` → **Run 'Tests in…'** (23 tests).

If an older version of the app is installed, its database is rebuilt automatically on first launch.

## Demo credentials (also one-tap buttons on the login screen)

| Role | Email | Password |
|---|---|---|
| Researcher (admin) | `Group14.ad@securehomes.ac.za` | `Admin@1234` |
| Consumer (Version A, 2 sample devices) | `user@securehomes.ac.za` | `User@1234` |

New accounts made with **Create one** are participants. Because the demo consumer is in Version A,
the next new account is assigned to Version B.

## Structure
```
app/src/main/java/com/securehomes/app/
├── models/     User, Device, DeviceCatalog, TelemetryEvent, SurveyResult, GroupStats, SecurityEvaluation
├── database/   DatabaseHelper (SQLite schema v2, CRUD, telemetry, analytics queries)
├── security/   PasswordHasher (PBKDF2), SessionManager, SecurityEvaluator (100-point score)
├── study/      StudyConfig (A/B assignment, factory state), SurveyScorer (SUS, NASA-TLX)
├── util/       Validators (email, passwords, IPv4, subnet, password strength)
└── ui/
    ├── common/     BaseActivity (session + role checks, logout)
    ├── auth/       LoginActivity, RegisterActivity
    ├── dashboard/  MainActivity, DeviceAdapter
    ├── wizard/     AddDeviceActivity, SetupRouter, SetupBaseActivity,
    │               GuidedWizardActivity (A), UnguidedFormActivity (B)
    ├── details/    DeviceDetailsActivity (audit, reconfigure, firmware, delete)
    ├── survey/     SurveyActivity
    └── analytics/  AnalyticsActivity, TelemetryAdapter
app/src/test/   JUnit tests (scoring, hashing, assignment, survey scoring, validation)
```

## Before collecting real study data
- Change the demo passwords, or remove the demo accounts in `DatabaseHelper.seedDatabase()`.
- Clear app storage so test data does not mix with real data.

## Assets
Device pictures (`camera.png`, `lock.png`, `plug.png`, `bulb.png`), `thermostat.xml` (vector) and the
app logo (`logo.png`) are in `app/src/main/res/drawable`.

## Group 14 contributors (Eduvos Midrand Campus)
- **Karabo Ngobeni** (EDUV4957217)
- **Regomoditswe Mathibe** (EDUV4967225)
- **Thina Dyantyi** (Pt.2022.g5k8d4)
