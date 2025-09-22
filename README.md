# Top Out

Recording yourself while bouldering is a great way to spot mistakes and improve your skills.
But using your camera app to record is pretty annoying, so I created something better!

## Pain Points

If you've ever used your phone to record your climbs, you know these problems:

1. **Overly long videos**

    Climbers need to rest between attempts, which means the sections of actual climbing are buried in a long video that's just recording the wall. This **wastes a lot of time** when reviewing, and not to mention valuable phone storage.

2. **Poor organization**

    Let's say you climb every three days. Your gallery will be a mix of videos of your greatest moments, funniest fails, along with **hundreds** of other photos and videos. Finding a specific attempt can take a while, and manually organizating clips takes forever.

## Top Out

> **Features:**
> - 🧗 Use Top Out to easily record your climbs
> - 🧠 Detect when each attempt starts and ends by using AI
> - 🚀 Saves time and storage
> - 📋 Great overview of all attempts of past climbs

## Demo

https://github.com/filippo-orru/top-out-app/raw/main/demo.mp4

*Demo of the app*

The home screen shows all your recordings, sorted by date. To create a recording, tap the plus button and position your phone on the ground. When you're ready, tap record! Later you can view all attempts done during the recording and edit them if necessary. By default, the app always saves the whole video, allowing you to edit *when exactly* an attempt starts and ends.

## How does it work?
The app uses **two machine learning models** to detect when you start and stop climbing. First, a segmentation model recognizes the gym **floor**. Then a pose detection model finds the **location of your feet**. When your feet leave the floor area for a bit, the attempt starts. When you top the route or fall, your feet will be back in the floor area, and the attempt is stopped.

## Tech Details

The Android app is written in Kotlin and uses [Jetpack Compose](https://developer.android.com/compose). Data about recordings and attempts is stored in a [Room database](https://developer.android.com/training/data-storage/room). 

The recording screen needs to interface with several components, including the camera feed, the [segmentation](https://ai.google.dev/edge/mediapipe/solutions/vision/interactive_segmenter) and [pose detection](https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker) ML models, and a helper class that smoothes the pose data using a [one euro filter](https://gery.casiez.net/1euro/). Every time an image is received from the camera feed or one of the models returns a result, an immutable state update is sent via a stream. The view layer listens to the update stream and recomposes itself when the state changes.

## Installation

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

### Prerequisites
- Android device with API level 24+ (Android 7.0)
- Camera permission for video recording

## Future

The app is currently not available to download.

In the future, the app could be extended to include social features, such as a local leaderboard. It could match routes visually, by comparing the positions of relevant holds.