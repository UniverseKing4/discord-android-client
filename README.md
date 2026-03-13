# Discord Android Client

A fully functional Android application with **OAuth2 user authentication** for interacting with the Discord API.

## Features

- **OAuth2 Login**: Secure user authentication with PKCE
- **Auto Token Refresh**: Seamless token renewal
- **Load Messages**: Fetch and display messages from any channel
- **Send Messages**: Post messages as the authenticated user
- **User Profile**: Display logged-in user information
- **Chrome Custom Tabs**: Secure browser-based login flow

## ⚠️ Setup Required

**You MUST configure OAuth2 before building!** See [OAUTH_SETUP.md](OAUTH_SETUP.md) for detailed instructions.

### Quick Setup

1. Create a Discord application at [Discord Developer Portal](https://discord.com/developers/applications)
2. Add redirect URI: `discord://oauth2/callback`
3. Copy your Client ID and Client Secret
4. Update `DiscordOAuth.kt` with your credentials
5. Build and install

## Installation

Download the latest APK from [Releases](https://github.com/UniverseKing4/discord-android-client/releases) or build from source.

**Requirements**: Android 7.0+ (API 24)

## Usage

1. Open the app
2. Tap "Login with Discord"
3. Authorize in your browser
4. Enter a channel ID
5. Load and send messages!

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Authentication**: OAuth2 with PKCE
- **Networking**: OkHttp for HTTPS
- **Async**: Kotlin Coroutines
- **JSON**: Gson
- **Browser**: Chrome Custom Tabs

## OAuth2 Implementation

This app implements the **Authorization Code Grant** flow with **PKCE** (Proof Key for Code Exchange) for enhanced security:

- State parameter for CSRF protection
- Code verifier/challenge for authorization code interception prevention
- Automatic token refresh
- Secure token storage

### Scopes

- `identify` - User information
- `guilds` - Server list
- `messages.read` - Read messages (requires Discord approval)

## API Integration

Uses Discord API v10:
- `GET /users/@me` - Current user
- `GET /channels/{id}/messages` - Fetch messages
- `POST /channels/{id}/messages` - Send messages

Authentication via `Bearer` tokens in the `Authorization` header.

## Build

Automated builds via GitHub Actions:
- APK compilation on every push
- Automatic versioning
- Release creation with downloadable APK

## Security

- PKCE prevents authorization code interception
- State parameter prevents CSRF attacks
- Tokens stored securely in app-private storage
- Chrome Custom Tabs for secure authentication

## License

This project is provided as-is for educational purposes. Follow Discord's Terms of Service and Developer Terms.
