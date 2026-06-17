#!/bin/bash
echo "🚀 Building Android Debug APK..."
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

echo "📱 Installing to connected device via USB..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "▶️ Launching Somewhere App..."
adb shell am start -n "com.somewhere.app/com.somewhere.app.MainActivity"

echo "✅ Deployment complete!"
