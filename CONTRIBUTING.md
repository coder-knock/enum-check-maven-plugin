# Contributing

We love contributions from everyone! Whether you're fixing a bug, adding a feature, or improving documentation, your help is welcome.

## How to Contribute

### 1. Fork & Clone

1. Fork the [repository](https://github.com/coder-knock/maven-enum-check-plugin/fork) on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/maven-enum-check-plugin.git
   cd maven-enum-check-plugin
   ```
3. Add the original repository as an upstream remote:
   ```bash
   git remote add upstream https://github.com/coder-knock/maven-enum-check-plugin.git
   ```

### 2. Create a Branch

Create a feature branch from `master`:
```bash
git checkout -b feature/your-amazing-feature
# or for bug fixes:
git checkout -b bugfix/fix-something-broken
```

### 3. Make Your Changes

- Follow the existing coding style
- Add tests for any new functionality
- Make sure all existing tests pass:
  ```bash
  mvn clean test
  ```
- Update documentation if needed

### 4. Commit Your Changes

Write good commit messages that clearly describe what changed:
```bash
git commit -m "Add amazing new feature: explanation of what changed"
```

### 5. Push to Your Fork

```bash
git push origin feature/your-amazing-feature
```

### 6. Open a Pull Request

Go to the original repository on GitHub and open a new Pull Request.

## Development Setup

### Prerequisites

- Java 8 or higher
- Maven 3.6 or higher

### Build the Project

```bash
# Build and run tests
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run only unit tests
mvn clean test
```

### Project Structure

```
src/
├── main/
│   └── java/io/github/coderknock/maven/plugin/enumcheck/
│       ├── annotation/          # Annotation definitions
│       │   ├── CheckGroup.java
│       │   └── EnumCheck.java
│       ├── CompositeDuplicateInfo.java
│       ├── DuplicateInfo.java
│       ├── EnumCheckMojo.java      # Maven plugin entry
│       └── SourceEnumChecker.java   # Core checking logic
└── test/
    └── java/
        ├── test/SourceEnumCheckerTest.java  # Unit tests
        └── test/enums/                 # Test enum examples
```

## Guidelines

### Code Style

- Please follow the existing coding conventions used in the project
- Keep code clean and well-commented
- Methods should be small and focused on a single responsibility

### Testing

- All new features should include unit tests
- Bug fixes should include tests that verify the bug is fixed
- Ensure all existing tests pass before submitting

### Documentation

- If you add a new feature, update the relevant documentation in README
- Keep documentation up to date with changes in code

## Reporting Issues

### Found a bug?

Please open a [GitHub Issue](https://github.com/coder-knock/maven-enum-check-plugin/issues/new) and include:

- A clear description of the bug
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Version of the plugin you're using
- Java and Maven versions

### Feature Requests

We love feature requests! Please open an issue and describe:

- What feature you'd like to see
- Why it's useful
- Any ideas about how it should work

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Questions?

If you have any questions, feel free to open an issue or contact the maintainer at opensource@coderknock.com.

Thank you for contributing! 🎉
