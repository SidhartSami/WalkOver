# Debugging Google Sign-In "Not Logging In"

## Status: 🚫 FEATURE REMOVED

## History
- **Issue**: Google Sign-In was blocked by email verification logic.
- **Fix Attempted**: Logic was patched to allow Google users.
- **Decision**: User decided to remove Google Sign-In entirely for now and re-introduce it later.

## Changes Implemented
- **LoginScreen.kt**: Removed "Continue with Google" button, related initialization logic, and imports.
- **FirebaseService.kt**: Left `signInWithGoogle` logic intact (but unused) for future restoration.

## Future Steps (if re-integrating)
1. Restore UI in `LoginScreen.kt`.
2. Ensure `FirebaseService.isUserAuthenticated()` allows Google users (fix is already in place).
3. Update specific Google Sign-In dependencies if needed (Credential Manager).
