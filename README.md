# TopOut - AI-Powered Climbing Analysis App

TopOut is an innovative Android application that uses computer vision and AI to analyze rock climbing performances. The app automatically detects climbing movements, records attempts, and provides detailed insights into your climbing sessions.

## Demo

https://github.com/filippo-orru/top-out-app/raw/main/demo.mp4

*Watch the demo video to see TopOut in action*

## Features

### 🎯 **Automatic Climbing Detection**
- Real-time pose detection using MediaPipe
- Intelligent climbing state recognition (idle, climbing, not detected)
- Automatic attempt segmentation based on movement patterns

### 📹 **Smart Video Recording**
- HD video recording of climbing sessions
- Automatic attempt detection and timestamping
- Video thumbnails for easy session browsing
- Individual attempt video clips with precise timing

### 📊 **Performance Analysis**
- Detailed tracking of climbing attempts
- Timestamp analysis for each climbing sequence
- Visual pose landmarks and movement tracking
- Historical session data with Room database

### 📱 **Modern UI**
- Clean Material 3 design
- Jetpack Compose interface
- Intuitive navigation between sessions
- Easy video playback and editing

## Technical Overview

TopOut leverages cutting-edge computer vision technology to provide accurate climbing analysis:

- **MediaPipe**: Google's ML framework for pose detection and body segmentation
- **Camera2 API**: High-quality video recording and real-time image processing
- **Room Database**: Local storage for climbing sessions and attempts
- **Jetpack Compose**: Modern Android UI toolkit
- **ExoPlayer**: Smooth video playback and editing

## Architecture

The app follows clean architecture principles with the following key components:

### Services
- `PoseDetectorService`: Handles real-time pose detection using MediaPipe
- `SegmentationService`: Processes body segmentation for enhanced accuracy
- `ClimbingStateService`: Analyzes pose data to determine climbing states

### Data Layer
- `RouteVisitEntity`: Stores climbing session information
- `AttemptEntity`: Tracks individual climbing attempts
- `Database`: Room database for local data persistence

### UI Layer
- `MainScreen`: Session overview and navigation
- `RecordScreen`: Live recording with real-time pose visualization
- `ViewRouteVisitScreen`: Session playback and analysis
- `CutScreen`: Video editing and attempt refinement

## Installation

### Prerequisites
- Android device with API level 24+ (Android 7.0)
- Camera permission for video recording
- Minimum 2GB RAM recommended for optimal ML performance

### Build from Source

1. Clone the repository:
```bash
git clone https://github.com/filippo-orru/top-out-app.git
cd top-out-app
```

2. Open in Android Studio or build with Gradle:
```bash
./gradlew assembleDebug
```

3. Install on device:
```bash
./gradlew installDebug
```

## Usage

### Starting a Recording Session
1. Launch TopOut and grant camera permissions
2. Tap the floating action button (➕) to start recording
3. Position your device to capture the climbing area
4. Begin climbing - the app will automatically detect your movements

### Viewing Sessions
1. Return to the main screen to see all recorded sessions
2. Tap on any session to view details
3. Browse individual attempts with thumbnails
4. Play back specific climbing sequences

### Editing Attempts
1. From a session view, tap on any attempt
2. Use the video editor to refine start/end times
3. Save your edits for accurate performance tracking

## Key Technologies

- **Kotlin**: Primary development language
- **Android Jetpack**: Modern Android development components
- **MediaPipe**: Real-time ML inference for pose detection
- **CameraX**: Camera API for video recording
- **Room**: Local database for data persistence
- **Compose**: Declarative UI framework
- **Material 3**: Modern design system

## Performance Features

- **Real-time Processing**: Live pose detection at camera frame rate
- **Efficient Storage**: Optimized video compression and database queries
- **Battery Optimization**: Smart processing to minimize power consumption
- **Memory Management**: Careful handling of video and ML model memory

## Data Privacy

TopOut processes all video and pose data locally on your device. No climbing data is sent to external servers, ensuring complete privacy of your climbing sessions.

## Contributing

Contributions are welcome! Please feel free to submit issues and enhancement requests.

## License

This project is open source. Please check the LICENSE file for details.

---

**Experience the future of climbing analysis with TopOut - where AI meets adventure!** 🧗‍♀️⛰️