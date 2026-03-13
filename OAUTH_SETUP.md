# Discord Android Client - OAuth2 Setup Guide

## IMPORTANT: OAuth2 Configuration Required

Before building the app, you **MUST** configure your Discord application OAuth2 settings.

### Step 1: Create Discord Application

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application"
3. Give it a name (e.g., "My Discord Android Client")
4. Click "Create"

### Step 2: Configure OAuth2

1. In your application, go to **OAuth2** → **General**
2. Copy your **Client ID** and **Client Secret**
3. Under **Redirects**, add: `discord://oauth2/callback`
4. Click "Save Changes"

### Step 3: Update App Code

Open `app/src/main/java/com/discord/client/DiscordOAuth.kt` and replace:

```kotlin
const val CLIENT_ID = "YOUR_CLIENT_ID"
const val CLIENT_SECRET = "YOUR_CLIENT_SECRET"
```

With your actual Client ID and Client Secret from Step 2.

### Step 4: Build and Install

```bash
./gradlew assembleRelease
```

The APK will be in `app/build/outputs/apk/release/`

## How It Works

### OAuth2 Authorization Code Flow with PKCE

This app implements the secure OAuth2 authorization code flow with PKCE (Proof Key for Code Exchange):

1. **User clicks "Login with Discord"**
   - App generates a random `state` and `code_verifier`
   - Creates `code_challenge` from the verifier using SHA-256
   - Opens Discord authorization page in Chrome Custom Tab

2. **User authorizes the app**
   - Discord redirects back to `discord://oauth2/callback` with authorization code
   - App validates the `state` parameter to prevent CSRF attacks

3. **App exchanges code for tokens**
   - Sends authorization code + code_verifier to Discord
   - Receives `access_token` and `refresh_token`
   - Tokens are securely stored in SharedPreferences

4. **Making API requests**
   - Uses `Bearer` token authentication
   - Automatically refreshes expired tokens
   - Handles 401 errors gracefully

### Security Features

- **PKCE**: Protects against authorization code interception
- **State parameter**: Prevents CSRF attacks
- **Secure storage**: Tokens stored in app-private SharedPreferences
- **Auto-refresh**: Seamless token renewal
- **Chrome Custom Tabs**: Secure browser-based authentication

## Usage

1. Open the app
2. Tap "Login with Discord"
3. Authorize the app in your browser
4. You'll be redirected back to the app
5. Enter a channel ID and start messaging!

## Scopes Requested

- `identify`: Get user information
- `guilds`: See user's servers
- `messages.read`: Read messages (requires approval from Discord)

**Note**: Some scopes like `messages.read` require Discord approval for production use.

## Troubleshooting

### "Login failed"
- Check that CLIENT_ID and CLIENT_SECRET are correct
- Verify redirect URI is exactly `discord://oauth2/callback`

### "Authentication expired"
- The app will automatically refresh tokens
- If refresh fails, log out and log in again

### "Failed: 403"
- Your app may not have permission for the requested scope
- Some scopes require Discord approval

## API Endpoints Used

- `GET /users/@me` - Get current user info
- `GET /channels/{id}/messages` - Fetch messages
- `POST /channels/{id}/messages` - Send messages

## License

Educational purposes only. Follow Discord's Terms of Service and Developer Terms.
