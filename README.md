# Discord Android Client

A fully functional Android application that provides a UI for interacting with the Discord API via HTTPS.

## Features

- **Load Messages**: Fetch and display the last 50 messages from any Discord channel
- **Send Messages**: Send messages to Discord channels using bot tokens
- **Real-time Display**: Messages are displayed in a scrollable list with author names
- **Clean UI**: Material Design 3 interface with Discord's signature colors

## Setup

1. **Get a Discord Bot Token**:
   - Go to [Discord Developer Portal](https://discord.com/developers/applications)
   - Create a new application
   - Navigate to the "Bot" section and create a bot
   - Copy the bot token

2. **Get Channel ID**:
   - Enable Developer Mode in Discord (Settings → Advanced → Developer Mode)
   - Right-click on any channel and select "Copy ID"

3. **Install the App**:
   - Download the latest APK from [Releases](https://github.com/UniverseKing4/discord-android-client/releases)
   - Install on your Android device (requires Android 7.0+)

## Usage

1. Open the app
2. Enter your bot token in the "Bot Token" field
3. Enter the channel ID in the "Channel ID" field
4. Tap "Load Messages" to fetch messages
5. Type a message and tap "Send" to post to the channel

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Single Activity with RecyclerView
- **Networking**: OkHttp for HTTPS requests
- **Async**: Kotlin Coroutines
- **JSON**: Gson for serialization

## API Integration

The app uses Discord API v10 endpoints:
- `GET /channels/{channel_id}/messages` - Fetch messages
- `POST /channels/{channel_id}/messages` - Send messages

Authentication is handled via the `Authorization` header with bot tokens.

## Build

The project uses GitHub Actions for automated builds. Every push to main triggers:
- APK compilation with Gradle
- Automatic versioning
- Release creation with downloadable APK

## License

This project is provided as-is for educational purposes.
