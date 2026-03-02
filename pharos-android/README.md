# Pharos - Local Document Analysis App

Pharos is a native Android app that scans local documents, analyzes them via AI (on-demand only), derives projects/topics, and generates Markdown masterfiles.

## Key Principles

- **No automatic background processing** - All analysis is triggered manually via button press
- **BYOK (Bring Your Own Key)** - App ships without API key; user provides their own Perplexity API key
- **Privacy-first** - All data stays local (Room DB + files in user-chosen folder). API key encrypted with Android Keystore
- **Minimal permissions** - Uses Storage Access Framework (SAF) instead of broad file access

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 34

### Build Steps

1. Clone the repository
2. Open the `pharos-android/` directory in Android Studio
3. Let Gradle sync dependencies
4. Build the project: `Build > Make Project`
5. Run on emulator or device (minSdk 26 / Android 8.0+)

### Build APK from command line

```bash
cd pharos-android
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

For release build:
```bash
./gradlew assembleRelease
```

## Architecture

```
com.pharos.app/
├── data/
│   ├── db/          # Room database, entities, DAOs
│   └── repository/  # Data repositories
├── domain/
│   └── usecase/     # Business logic (Scan, Analysis, Clustering, Masterfile)
├── network/
│   └── api/         # AI API provider abstraction + Perplexity implementation
├── ui/
│   ├── screen/      # Compose screens (Dashboard, Folders, Files, Projects, Settings)
│   ├── navigation/  # Navigation routes
│   └── theme/       # Material3 theme
└── util/            # Text extraction, PDF handling, JSON parsing, filename sanitization
```

### Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVVM
- **Database:** Room
- **Networking:** OkHttp
- **PDF:** PDFBox-Android
- **Security:** EncryptedSharedPreferences + Android Keystore
- **File Access:** Storage Access Framework (SAF)

## Screens

### Dashboard
- Shows file statistics (indexed, new/changed, analyzed)
- Three action buttons:
  - **Scan Folder** - Indexes documents locally (no API calls)
  - **Start Analysis** - Sends documents to AI API (requires confirmation dialog showing file count and estimated tokens)
  - **Update Masterfiles** - Generates/updates Markdown files locally (no API calls)
- Progress bars with cancel support for all operations

### Folders
- SAF folder picker (`ACTION_OPEN_DOCUMENT_TREE`)
- Persists URI permissions for continued access
- Currently supports one folder (model supports multiple)

### Files
- Lists all indexed files with status icons:
  - **NEVER** - Not yet analyzed
  - **UP_TO_DATE** - Analysis current
  - **STALE** - File changed since last analysis
  - **FAILED** - Analysis failed
  - **UNSUPPORTED** - Cannot extract text
- File detail view shows analysis results (summary, topics, action items, confidence)
- Per-file "Analyze This File" button

### Projects
- Auto-generated from analysis results via clustering
- Shows project name, file count, last update date
- Project detail shows assigned files with summaries and topics

### Settings
- API key management (save, test, delete)
- Toggle "Only changed files" analysis mode
- Privacy notice

## BYOK & Privacy

- **Your API key is never logged or transmitted anywhere except to the configured AI API endpoint**
- The key is stored in EncryptedSharedPreferences backed by Android Keystore
- Document content is sent to the AI API **only** when you explicitly press "Analyze"
- No background syncs, no automatic uploads, no analytics
- All project data and analysis results stay in the local Room database

## Supported File Formats (MVP)

| Format | Extension | Method |
|--------|-----------|--------|
| Plain text | `.txt` | Direct read |
| Markdown | `.md` | Direct read |
| PDF | `.pdf` | PDFBox text extraction |

PDF files where text cannot be extracted are marked as UNSUPPORTED.

## AI API

Currently supports **Perplexity AI** (`sonar` model). The provider layer is abstracted via the `AiApiProvider` interface, making it straightforward to add other providers.

### Analysis Output Schema
```json
{
  "topics": ["topic1", "topic2"],
  "project_suggestions": ["Project Name"],
  "summary": "Document summary...",
  "action_items": ["Action 1", "Action 2"],
  "confidence": 0.85
}
```

## Masterfiles

Generated in the selected folder as `PHAROS_MASTER_<ProjectName>.md`:
- Project title and description
- Last update timestamp
- Assigned files with summaries and topics
- Aggregated action items as checklist

## Testing

```bash
cd pharos-android
./gradlew test
```

Unit tests cover:
- JSON parsing (valid, invalid, code-fenced responses)
- Filename sanitization (special chars, umlauts, length limits)
- Project name normalization
