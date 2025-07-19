# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build commands
./gradlew build                    # Build entire project
./gradlew assembleDebug           # Build debug APK
./gradlew clean                   # Clean build directory

# Testing commands
./gradlew test                    # Run unit tests
./gradlew connectedAndroidTest    # Run instrumented tests on device
./gradlew connectedCheck          # Run all device tests
./gradlew check                   # Run all verification tasks
./gradlew lint                    # Run code analysis

# Installation
./gradlew installDebug            # Install debug build on connected device
```

## Project Architecture

**InkRecorder** is an Android audio recording application built with modern Android architecture patterns:

### Core Architecture
- **MVVM + MVI**: ViewModels with Model-View-Intent pattern using FlowRedux state machines
- **Clean Architecture**: Clear separation of data, domain, and UI layers
- **Dependency Injection**: Dagger Hilt for dependency management

### Key Technologies
- **UI**: Jetpack Compose with Material Design 3
- **State Management**: FlowRedux state machines for complex flows (especially permission handling)
- **Audio**: Real-time PCM audio recording (48kHz, 16-bit, mono) with custom waveform visualization
- **Reactive Programming**: StateFlow/SharedFlow with Coroutines

### Package Structure
```
com.outaink.inkrecorder/
├── MyApplication.kt              # Hilt application entry
├── data/                         # Data layer
│   ├── audio/                   # Audio recording & streaming interfaces
│   └── discovery/               # Network service discovery
├── di/                          # Hilt dependency injection modules
├── ui/                          # UI layer (MVVM + MVI)
│   ├── MainActivity.kt          # Main activity
│   ├── PermissionStateMachine.kt # FlowRedux permission handling
│   ├── Waveform.kt              # Custom animated waveform component
│   └── mic/                     # Microphone recording feature
└── utils/                       # Utility classes
```

## Key Components

### Audio System
- **AudioRecorder**: Interface and implementation for high-quality audio recording
- **AudioStreamer**: Network-based audio streaming (interface defined)
- **Waveform Component**: Advanced real-time animated visualization with multiple draw modes (Bars, Line, Mirror, Gradient)

### Permission Management
- **PermissionStateMachine**: FlowRedux-based state machine for handling complex permission flows
- Uses Accompanist Permissions library for Compose integration

### State Management Patterns
- **MicViewModel**: Central ViewModel using MVI pattern with FlowRedux
- **WaveformData**: Data model for amplitude data and playback progress
- Reactive state updates using StateFlow/SharedFlow

## Development Notes

### Audio Recording Configuration
- Sample Rate: 48kHz
- Bit Depth: 16-bit
- Channels: Mono (1 channel)
- Format: PCM
- Real-time waveform data processing and visualization

### Waveform Component Features
- Smooth animations on data updates using spring animations
- Multiple visualization modes (bars, lines, mirror, gradient)
- Configurable animation intensity and styling
- Progress tracking for playback visualization

### FlowRedux Usage
The project uses FlowRedux state machines for managing complex state transitions, particularly:
- Permission request flows
- Audio recording state management
- UI state coordination

When working with state machines, follow the established MVI pattern:
1. Define clear State, Intent, and Effect contracts
2. Use sealed classes/interfaces for type safety
3. Handle state transitions in the state machine's reduce function

### Testing Structure
- Unit tests in `/app/src/test/`
- Instrumented tests in `/app/src/androidTest/`
- Uses JUnit 4.13.2 and Espresso for testing framework

### Build Configuration
- Target SDK: 36, Min SDK: 30
- Kotlin 2.1.21 with Java 11 compatibility
- Android Gradle Plugin 8.11.0
- Uses version catalogs for dependency management