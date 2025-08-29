# Contributing to DrishtiPay POS SDK

DrishtiPay POS SDK is a free and open-source project, and we love to receive contributions from our community — you!
There are many ways to contribute, from writing tutorials, improving the documentation, submitting bug reports
and feature requests, creating new POS device plugins, or writing code which can be incorporated into the SDK itself.

If you are excited and want to make contributions, sign up for the
[Freedom Finance Stack Contributor Program](https://razorpay.com/).

## 🚀 Ways to Contribute

- **Plugin Development**: Create plugins for new POS hardware manufacturers (PAX, Ingenico, Verifone, etc.)
- **Core SDK Enhancement**: Improve NFC handling, GGWave integration, or add new features
- **Documentation**: Write tutorials, improve API docs, or create integration guides
- **Testing**: Add unit tests, integration tests, or test on different POS devices
- **Bug Reports**: Report issues with existing functionality
- **Examples**: Create sample applications demonstrating SDK usage

### Fork and clone the repository
* You will need to fork the main DrishtiPay POS SDK repository and clone it to your local machine.
  See [github help page](https://docs.github.com/en/get-started/quickstart/fork-a-repo) for help.
    * Follow the [build steps](README.md#-build-requirements) for local setup.

### Creating an Issue

Before **creating** an Issue for `features`/`bugs`/`improvements` please follow these steps:

1. Search existing Issues before creating a new issue (has someone raised this already).
2. All Issues are automatically given the label status: waiting for triage and are automatically locked so no comments can be made
3. If you wish to work on the Issue once it has been triaged and label changed to status: ready for dev, please include this in your Issue description.

### Working on an Issue (get it assigned to you)
Before working on an existing Issue please follow these steps:
1. Only ask to be assigned 1 **open** issue at a time
2. Comment asking for the issue to be assigned to you (reviewers/maintainers are auto assigned and we recommend not to tag do not tag all maintainers on GitHub as all maintainers receive your comment notifications)
3. After the Issue is assigned to you, you can start working on it
4. **Only** start working on this Issue (and open a Pull Request) when it has been assigned to you - this will prevent confusion, multiple people working on the same issue and work not being used
5. Reference the Issue in your Pull Request (for example `closes #<ISSUE NUMBER> `)
6. Please do **not** force push to your PR branch, this makes it very difficult to re-review - commits will be squashed when merged

## Submitting Bug/Feature Reports
When initiating a new issue in the DrishtiPay POS SDK issue tracker, you'll encounter a template designed to streamline the reporting process. If you suspect you've identified a bug, please complete the form following the template to the best of your ability. Don't worry if you can't address every detail; provide information where possible.

For a comprehensive evaluation of the report, we primarily need:
- **Device Information**: POS device model, Android version, SDK version
- **Plugin Details**: Which POS plugin you're using (PAX, Ingenico, etc.)
- **Reproduction Steps**: Clear steps to reproduce the issue
- **Expected vs Actual Behavior**: What should happen vs what actually happens
- **Logs**: Relevant log output (with PII redacted)
- **Test Case**: A minimal code example that demonstrates the problem

The ability to recreate the issue is crucial for us to effectively diagnose and address it. Your cooperation in providing these details greatly assists our debugging efforts.

### Triaging a Bug/Feature Report
* After initiating an issue, it's common to engage in discussions, especially when contributors hold differing opinions on whether the observed behavior is a bug or a feature. This dialogue is an integral part of the process and should maintain a focused, constructive, and professional tone.

* It's important to avoid short, abrupt responses that lack additional context or supporting details, as they can be perceived as unhelpful and unfriendly. Contributors are encouraged to contribute to a positive and collaborative environment by providing constructive input and assisting each other in making progress.

* If you find an issue that you believe doesn't need fixing or if you encounter information you think is incorrect, share your perspective with additional context and be open to being convinced otherwise. This approach helps us collectively reach accurate resolutions more efficiently.

### Resolving a Bug/Feature Report
In most instances, resolving issues involves the creation of a Pull Request (PR). The steps for initiating and reviewing a PR mirror those of opening and triaging issues. However, the PR process includes a crucial review and approval workflow to guarantee that the proposed changes adhere to the minimal quality and functional standards set by the DrishtiPay POS SDK project.

### Raising a PR
When creating a new Pull Request on GitHub, you'll encounter a template that should be completed. While it's encouraged to provide as much detail as possible, feel free to omit sections if you're uncertain about the information to be included. Aim to complete the template to the best of your ability.

## Contributing code and documentation changes
If you would like to contribute a new feature or a bug fix to DrishtiPay POS SDK, please discuss your idea first on the GitHub issue.
The process for contributing to DrishtiPay POS SDK can be found below.

### 🔌 Plugin Development Guidelines
When creating a new POS device plugin:

* Implement the `IPosNfcPlugin` interface completely
* Follow the plugin architecture patterns shown in existing examples
* Test with both mock and real hardware (if available)
* Document supported device models and firmware versions
* Include proper error handling and logging (no PII)
* Add plugin-specific examples and documentation

### Tips for code changes
Following these tips prior to raising a pull request will speed up the review cycle.

* **Add appropriate unit tests** - Test core logic, threading, and error conditions
* **Add integration tests** - Test with mock POS devices and GGWave functionality
* **Follow Android coding conventions** - Use meaningful names, proper Javadoc
* **Ensure thread safety** - All callbacks must be thread-safe
* **Test on different API levels** - Minimum API 23, target API 36
* **No PII in logs** - Never log sensitive payment or personal information
* **Lines that are not part of your change should not be edited** (e.g. don't format unchanged lines)
* **Add appropriate license headers** to any new files

### Contributing to the DrishtiPay POS SDK codebase
- Select the area to contribute (core SDK, plugins, examples, documentation).
- Fork the repo into your GitHub account.
- Clone the repo which you forked.
- Make the changes following our guidelines.
- Test your changes thoroughly.
- Push and create a PR for the issue/feature.
### Submitting your changes

Once your changes and tests are ready to submit for review:
1. **Test your changes thoroughly**:
   ```bash
   ./gradlew :pos-sdk-core:assemble :pos-sdk-core:lint :pos-sdk-core:test
   ```
2. **Take the latest pull from master**
3. **Create new branch and checkout to the new branch**
4. **Commit your changes on the new branch** using conventional commit format:
   ```
   feat: add support for Ingenico POS devices
   fix: resolve NFC callback threading issue
   docs: update plugin development guide
   test: add unit tests for GGWave integration
   ```
5. **Submit a pull request**
6. > **IMPORTANT**: Before pushing your changes please run lint checks, otherwise build can fail.

***

## 🔧 Development Environment Setup

### Required Tools
- **Android Studio**: Arctic Fox or later
- **Java**: JDK 11 or later
- **Android SDK**: API 23+ (minSdk), API 36 (target/compile)
- **Gradle**: 7.0+

### Build Commands
```bash
# Build the core library
./gradlew :pos-sdk-core:assemble

# Run all checks
./gradlew :pos-sdk-core:assemble :pos-sdk-core:lint :pos-sdk-core:test

# Generate AAR for distribution
./gradlew :pos-sdk-core:assembleRelease
```

### Code Style Guidelines

DrishtiPay POS SDK follows standard Android development practices:

1. **Java Conventions**: Follow standard Java naming and formatting conventions
2. **Javadoc**: All public APIs must have comprehensive Javadoc
3. **Null Safety**: Use `@NonNull` and `@Nullable` annotations
4. **Threading**: Document callback threading behavior
5. **Error Handling**: Provide meaningful error messages
6. **Privacy**: Never log PII, PAN, or sensitive payment data

### Testing Guidelines

- **Unit Tests**: Test core logic, edge cases, and error conditions
- **Integration Tests**: Test plugin interactions and GGWave functionality
- **Mock Testing**: Use mock mode for testing without real hardware
- **Thread Safety**: Test concurrent operations and callback handling
- **API Compatibility**: Test across supported Android API levels (23-36)

> **Note**: Persistent non-compliance with this Contributing Guide can lead to a warning and/or ban under our Code of Conduct. We strive to maintain a welcoming and professional community for all contributors.