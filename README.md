# grokAssist

[🇬🇧 English](README.md) | [🇨🇳 中文](README.zh.md)


grokAssist is a simple Android WebView wrapper for [Grok](https://grok.com/), providing a lightweight way to access the Grok AI chat interface on Android devices, including those without Google Mobile Services (GMS). This app is built by adapting the open-source code from [gptAssist](https://github.com/woheller69/gptAssist), and we extend our gratitude to the original author for their foundational work.

## Features

- Loads the Grok web interface[](https://grok.com/) in a WebView.
- Simple and lightweight, designed for seamless interaction with Grok.

## Notes

- For initial sign-up or login, you may need to use a web browser, as some authentication flows (e.g., via X or Google) might require turning off restricted mode.

## Installation

- Download the APK from releases or build it yourself using the instructions below.
- The app is not yet available on F-Droid, but contributions to make it available are welcome.

## Building

1. Clone the repository:
   ```bash
   git clone https://github.com/dmxystudio/grokassist.git

2. Open the project in Android Studio.
3. Sync the Gradle project and build the APK.
4. Install the APK on your Android device (minSdk 21, Android 5.0+).


## License

This app is licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).

The app is based on:
- [gptAssist](https://github.com/woheller69/gptAssist) by woheller69, licensed under GPLv3.
- Parts from [GMaps WV](https://gitlab.com/divested-mobile/maps), also licensed under GPLv3.

## Contributing

Contributions are welcome! If you find a bug or have a feature request, please follow these steps:

- **Report Issues**: Open an issue in the GitHub repository, ensuring no duplicate exists.
  - Clearly describe the issue, including steps to reproduce.
  - Include your Android version, device model, and screenshots (if applicable).
  - Be precise in your description.
- **Submit Fixes**: If you have a solution, comment on the relevant issue and, if possible, submit a pull request with your changes.

## Acknowledgments

A huge thank you to [woheller69](https://github.com/woheller69) for their open-source [gptAssist](https://github.com/woheller69/gptAssist) project, which served as the foundation for grokAssist. Their work made this adaptation possible.
