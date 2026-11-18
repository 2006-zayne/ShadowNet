#!/usr/bin/env bash

# ==============================================================================
# Scientific Calculator - Android Build & Deploy Script for ADB Devices
# ==============================================================================

set -e

# Colors for terminal formatting
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   Shadownet Scientific Calculator - ADB Deploy Tool  ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Clean environment variables that conflict with Android Gradle Plugin
unset ANDROID_PREFS_ROOT

# Configure Environment Paths (JDK 21 & Android SDK)
export JAVA_HOME="/home/zayne/.jdks/jbr-21.0.11"
export ANDROID_HOME="/home/zayne/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Locate Gradle executable
GRADLE_LOCAL="/home/zayne/.gradle/wrapper/dists/gradle-9.7.1-bin/1w1c7tv4s851m17nbqdsro2tv/gradle-9.7.1/bin/gradle"
if [ -x "$GRADLE_LOCAL" ]; then
    GRADLE_CMD="$GRADLE_LOCAL"
else
    GRADLE_CMD="./gradlew"
fi

echo -e "${YELLOW}[1/4] Checking Environment...${NC}"
if [ ! -d "$JAVA_HOME" ]; then
    echo -e "${RED}Error: JAVA_HOME not found at $JAVA_HOME${NC}"
    exit 1
fi

if [ ! -d "$ANDROID_HOME" ]; then
    echo -e "${RED}Error: ANDROID_HOME not found at $ANDROID_HOME${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java JDK:${NC} $(java -version 2>&1 | head -n 1)"
echo -e "${GREEN}✓ Android SDK:${NC} $ANDROID_HOME"
echo -e "${GREEN}✓ Gradle Executable:${NC} $GRADLE_CMD"

# 2. Check for Connected ADB Device
echo -e "\n${YELLOW}[2/4] Detecting Connected ADB Devices...${NC}"
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo -e "${RED}Error: No ADB device detected.${NC}"
    echo -e "Please connect your Android device via USB, enable 'USB Debugging' in Developer Options, and try again."
    adb devices
    exit 1
fi

DEVICE_ID=$(echo "$DEVICES" | head -n 1)
echo -e "${GREEN}✓ Connected Device Found:${NC} $DEVICE_ID"

# 3. Build Debug APK
echo -e "\n${YELLOW}[3/4] Building Debug APK with Gradle...${NC}"
"$GRADLE_CMD" assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}Error: APK build failed or file not found at $APK_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✓ APK Built Successfully:${NC} $APK_PATH"

# 4. Install and Launch Application on Device
echo -e "\n${YELLOW}[4/4] Installing and Launching Application on Device ($DEVICE_ID)...${NC}"
adb -s "$DEVICE_ID" install -r "$APK_PATH"

PACKAGE_NAME="com.shadownet.calculator"
MAIN_ACTIVITY=".MainActivity"

echo -e "${BLUE}Starting $PACKAGE_NAME$MAIN_ACTIVITY...${NC}"
adb -s "$DEVICE_ID" shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}  ✓ APP SUCCESSFULLY DEPLOYED AND LAUNCHED ON DEVICE! ${NC}"
echo -e "${GREEN}======================================================${NC}"
