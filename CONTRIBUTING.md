# Contributing

Thank you for your interest in contributing to Android Screensaver!

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/vincentwetzel/AndroidScreensaver/issues)
2. Open a new issue with:
   - Clear description of the problem
   - Steps to reproduce
   - Expected behavior
   - Actual behavior
   - Device info (model, Android version)
   - Screenshots if applicable

### Suggesting Features

1. Check existing issues for similar suggestions
2. Open a new feature request with:
   - Clear description of the feature
   - Use case / problem it solves
   - Any implementation ideas

### Code Contributions

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Write tests for new code
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to your branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Write unit tests for new features
- Ensure all tests pass before submitting PR

### Pull Request Guidelines

- Describe what your PR does
- Reference related issues (e.g., "Fixes #123")
- Include screenshots for UI changes
- Keep PRs focused and reasonably sized
- Update documentation if needed

## Development Setup

1. Fork and clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator

### Required Setup

Before running the app:
1. Create a Google Cloud project
2. Enable Google Drive API
3. Create OAuth credentials
4. Add your `google-services.json` to `app/` directory (do NOT commit)

See `GOOGLE_CLOUD_SETUP.md` for detailed instructions.

## Project Structure

```
app/
├── src/main/java/.../
│   ├── data/           # Data models and repositories
│   ├── di/             # Hilt dependency injection
│   ├── dream/          # DreamService (screensaver)
│   ├── ui/             # Activities and fragments
│   ├── utils/          # Utility classes
│   └── viewmodel/      # ViewModels
├── src/main/res/       # Resources
└── src/test/           # Unit tests
```

## Testing

- Run unit tests: `./gradlew test`
- Run instrumented tests: Connect device and run `./gradlew connectedAndroidTest`

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
