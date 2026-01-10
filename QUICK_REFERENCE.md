# Developer Quick Reference

## Project Commands

### Build
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew build                  # Build all variants
```

### Install
```bash
./gradlew installDebug           # Install debug APK to connected device
./gradlew installRelease         # Install release APK to connected device
```

### Test
```bash
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumentation tests
```

### Clean
```bash
./gradlew clean                  # Clean build directory
```

## Project Files

### Core Application Files
| File | Purpose |
|------|---------|
| `app/src/main/java/com/example/setsolver/MainActivity.kt` | Main activity - camera and UI |
| `app/src/main/java/com/example/setsolver/Card.kt` | Card data model |
| `app/src/main/java/com/example/setsolver/CardDetector.kt` | OpenCV card detection |
| `app/src/main/java/com/example/setsolver/SetFinder.kt` | Set-finding algorithm |
| `app/src/main/java/com/example/setsolver/ResultOverlayView.kt` | Custom overlay view |
| `app/src/main/java/com/example/setsolver/ImageUtils.kt` | Image utilities |

### Configuration Files
| File | Purpose |
|------|---------|
| `app/build.gradle` | App dependencies and configuration |
| `build.gradle` | Project-level Gradle configuration |
| `settings.gradle` | Project settings |
| `gradle.properties` | Gradle properties |
| `app/src/main/AndroidManifest.xml` | App manifest |

### Resource Files
| Directory | Contents |
|-----------|----------|
| `app/src/main/res/layout/` | XML layout files |
| `app/src/main/res/values/` | Strings, colors, themes |
| `app/src/main/res/drawable/` | Vector drawables |
| `app/src/main/res/mipmap-*/` | Launcher icons |

### Test Files
| File | Purpose |
|------|---------|
| `app/src/test/java/com/example/setsolver/SetFinderTest.kt` | Set finder unit tests |
| `app/src/test/java/com/example/setsolver/CardTest.kt` | Card model unit tests |

## Key Classes

### Card
```kotlin
data class Card(
    val number: Number,      // ONE, TWO, THREE
    val shape: Shape,        // DIAMOND, OVAL, SQUIGGLE
    val color: CardColor,    // RED, GREEN, PURPLE
    val shading: Shading,    // SOLID, STRIPED, OPEN
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)
```

### SetFinder
```kotlin
class SetFinder {
    fun isValidSet(card1: Card, card2: Card, card3: Card): Boolean
    fun findAllSets(cards: List<Card>): List<Triple<Card, Card, Card>>
}
```

### CardDetector
```kotlin
class CardDetector {
    fun detectCards(bitmap: Bitmap): List<Card>
}
```

## Common Tasks

### Adding a New Dependency
1. Open `app/build.gradle`
2. Add to `dependencies` block
3. Sync Gradle
4. Import in code

### Changing App Name
Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your New Name</string>
```

### Changing Package Name
1. Refactor package in Android Studio
2. Update `namespace` in `app/build.gradle`
3. Update `package` in `AndroidManifest.xml`

### Adding Permissions
Edit `app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.YOUR_PERMISSION" />
```

### Changing Colors
Edit `app/src/main/res/values/colors.xml`

### Changing App Icon
Replace files in `app/src/main/res/mipmap-*/`

## Debugging

### View Logs
```bash
adb logcat | grep -E "(MainActivity|CardDetector|SetFinder)"
```

### Common Issues

#### OpenCV Not Found
- Ensure OpenCV AAR is in `app/libs/`
- Check `app/build.gradle` has correct path
- Clean and rebuild project

#### Camera Permission Denied
- Uninstall app
- Reinstall and grant permission
- Or manually enable in device settings

#### Build Fails
- Check Android SDK is installed
- Check `ANDROID_HOME` is set
- Run `./gradlew clean`
- Sync Gradle files
- Update dependencies if needed

## Code Style Guidelines

### Kotlin
- Use meaningful names
- Prefer `val` over `var`
- Use data classes for immutable data
- Use companion objects for constants
- Add KDoc comments for public APIs

### Layout
- Use ConstraintLayout for complex layouts
- Use descriptive IDs
- Extract strings to `strings.xml`
- Extract colors to `colors.xml`
- Use dp for sizes, sp for text

### Best Practices
- Handle errors gracefully
- Request permissions at runtime
- Clean up resources (close, release)
- Use coroutines for async work
- Write unit tests for business logic

## Resources

- [Android Developer Docs](https://developer.android.com/)
- [Kotlin Docs](https://kotlinlang.org/docs/)
- [CameraX Guide](https://developer.android.com/training/camerax)
- [OpenCV Android](https://opencv.org/android/)
- [Material Design](https://material.io/design)

## Getting Help

- Check [BUILD.md](BUILD.md) for setup issues
- Read [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines
- Check [GAME_RULES.md](GAME_RULES.md) for Set game rules
- Create an issue on GitHub for bugs or questions
