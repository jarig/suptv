# Contributing to SupTV

Thank you for your interest in contributing to SupTV! This document provides guidelines for contributing to the project.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/suptv.git
   cd suptv
   ```
3. **Set up your development environment**:
   - Install JDK 17 or higher
   - Install Android Studio (for Android development)
   - Sync Gradle dependencies

## Development Setup

### Prerequisites
- JDK 17+
- Android SDK (API 34)
- Gradle 8.5 (via wrapper)

### Building the Project

```bash
# Build all modules
./gradlew build

# Build Android TV app
./gradlew :androidApp:assembleDebug

# Install on connected device/emulator
./gradlew :androidApp:installDebug
```

## Project Structure

- `shared/` - Kotlin Multiplatform module (common business logic)
  - `commonMain/` - Platform-agnostic code
  - `androidMain/` - Android-specific implementations
- `androidApp/` - Android TV application
  - `src/main/kotlin/` - Kotlin source code
  - `src/main/res/` - Android resources

## Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions small and focused
- Write tests for new features

## Submitting Changes

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**:
   - Write clean, well-documented code
   - Add tests for new functionality
   - Update documentation as needed

3. **Commit your changes**:
   ```bash
   git add .
   git commit -m "Add feature: description of your changes"
   ```

4. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create a Pull Request** on GitHub

## Pull Request Guidelines

- Provide a clear description of the changes
- Reference any related issues
- Ensure all tests pass
- Update documentation if needed
- Keep commits focused and atomic
- Write meaningful commit messages

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run shared module tests
./gradlew :shared:test

# Run Android tests
./gradlew :androidApp:testDebugUnitTest
```

### Writing Tests

- Place tests in the appropriate test directory
- Use descriptive test names
- Test edge cases and error conditions
- Aim for good test coverage

## Areas for Contribution

### High Priority
- VOD (Video on Demand) support
- EPG (Electronic Program Guide) integration
- Channel search and filtering
- Favorites management
- Settings screen

### Platform Support
- iOS implementation
- macOS implementation
- Windows implementation

### Improvements
- Performance optimizations
- UI/UX enhancements
- Additional playlist format support
- Better error handling
- Documentation improvements

## Questions?

If you have questions or need help:
- Open an issue on GitHub
- Check existing documentation (README.md, ARCHITECTURE.md, EXAMPLES.md)
- Review existing code for examples

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Follow project guidelines

Thank you for contributing to SupTV!
