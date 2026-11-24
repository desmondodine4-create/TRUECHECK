# TRUECHECK Firebase Backend Package

This folder contains the deployable Firebase configuration (`firebase-blueprint.json`, `firestore.rules`, and Cloud Functions under `functions/`) designed for TRUECHECK when connected to a Firebase project.

Because Firebase provisioning in AI Studio was declined during this session, the Android APK operates cleanly in local demonstration mode (Room + DataStore + `PlaceholderAiInspectionRepository`) without embedding API keys or unprovisioned SDK configs.

## Deploying to Your Own Firebase Project

1. **Install Firebase CLI & Authenticate**
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase use --add <YOUR_FIREBASE_PROJECT_ID>
   ```

2. **Initialize Firebase Hosting / Firestore / Functions**
   Place `firestore.rules` at the root of your Firebase deployment directory (`firebase.json` pointing `"firestore": { "rules": "firestore.rules" }` and `"functions": { "source": "functions" }`).

3. **Install Cloud Function Dependencies & Deploy**
   ```bash
   cd functions
   npm install
   cd ..
   firebase deploy --only firestore:rules,functions
   ```

4. **Assign Initial Root Administrator Claim**
   Consumer registrations strictly enforce `role = "CONSUMER"`. To elevate your initial operator account:
   - Use the Firebase Console or an Admin SDK script (`admin.auth().setCustomUserClaims(uid, { admin: true })` and set `users/{uid}.role = "ADMIN"`).

5. **Enable Firebase AI Logic & App Check**
   - In the Firebase Console, enable **Firebase AI Logic** (Gemini Developer API) and **App Check** (Play Integrity for release builds, Debug provider for local testing).
   - Swap `PlaceholderAiInspectionRepository` in `AppContainer.kt` with a `FirebaseAiInspectionRepository` implementation backed by `Firebase.ai(...)`.
