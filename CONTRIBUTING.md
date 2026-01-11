# Contributing to Set Solver

Thank you for your interest in contributing to Set Solver! This document provides guidelines for contributing to the project.

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:
- A clear description of the problem
- Steps to reproduce the issue
- Expected vs actual behavior
- Device information (Android version, device model)
- Screenshots if applicable

### Suggesting Enhancements

We welcome enhancement suggestions! Please create an issue with:
- A clear description of the enhancement
- Use cases and benefits
- Any potential implementation ideas

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Write or update tests as needed
5. Ensure all tests pass
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to your branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## Development Setup

See [BUILD.md](BUILD.md) for detailed setup instructions.

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions focused and small
- Use data classes for immutable data

## Testing

- Write unit tests for new features
- Ensure existing tests still pass
- Test on multiple Android versions if possible
- Test with real Set cards for card detection features

## Areas for Contribution

Here are some areas where contributions would be especially valuable:

### Card Detection Improvements
- Improve accuracy of card boundary detection
- Better handling of different lighting conditions
- Support for rotated cards
- Handle overlapping cards

### Pattern Recognition
- More accurate shape detection
- Better color recognition in various lighting
- Improved shading detection
- Machine learning-based pattern recognition

### User Experience
- Real-time card detection (without capture button)
- Visual feedback during detection
- Tutorial or help screens
- Statistics and game tracking
- Sound effects

### Performance
- Optimize image processing
- Reduce memory usage
- Faster set-finding algorithm for large numbers of cards

### Testing
- Add more unit tests
- Add instrumentation tests
- Add UI tests
- Test coverage improvements

## Questions?

Feel free to create an issue if you have questions about contributing!
