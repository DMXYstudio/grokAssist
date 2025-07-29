# grokAssist 简介

**grokAssist** 是一个简单的 Android WebView 封装应用，用于访问 [Grok](https://grok.com/) 的 AI 聊天界面。它为包括没有 Google 移动服务（GMS）的 Android 设备提供了轻量级访问方式。本项目基于开源项目 [gptAssist](https://github.com/woheller69/gptAssist) 修改而来，特此感谢原作者为本项目打下的基础。

## 功能特色

- 通过 WebView 加载 Grok 网页界面 [grok.com](https://grok.com/)。
- 界面简洁、运行轻便，专为流畅交互体验设计。

## 注意事项

- 第一次注册或登录账号时，部分认证流程（如使用 X 或 Google 登录）可能要求关闭 WebView 的“限制模式”，建议使用浏览器完成初始登录。

## 安装方式

- 你可以从 Releases 页面下载 APK 安装包，也可以参考下方方法自行构建。
- 目前尚未上架 F-Droid，欢迎社区协助推动上线。

## 构建方法

1. 克隆本项目仓库：
   ```bash
   git clone https://github.com/dmxystudio/grokassist.git
   ```

2. 用 Android Studio 打开项目。
3. 同步 Gradle 配置并构建 APK。
4. 安装生成的 APK 到你的 Android 设备上（最低支持 Android 5.0 / API 21）。

## 开源协议

本应用遵循 [GNU 通用公共许可证 第三版（GPLv3）](LICENSE) 开源发布。

本项目基于以下开源项目构建：
- [gptAssist](https://github.com/woheller69/gptAssist)，原作者 woheller69，遵循 GPLv3 协议。
- 部分代码参考自 [GMaps WV](https://gitlab.com/divested-mobile/maps)，同样遵循 GPLv3 协议。

## 贡献指南

欢迎任何形式的贡献！如果你发现问题或有功能建议，请按照以下流程提交：

- **反馈问题**：在 GitHub 仓库中开启新 issue，确保没有重复问题。
  - 清晰描述问题发生的过程。
  - 请附带 Android 系统版本、设备型号、截图（如适用）。
  - 描述越具体越容易重现。
- **提交修复**：如你有解决方案，请在 issue 中留言，或提交 Pull Request 带上你的修改内容。

## 鸣谢

特别感谢 [woheller69](https://github.com/woheller69) 所开发的开源项目 [gptAssist](https://github.com/woheller69/gptAssist)，为 grokAssist 提供了开发的起点和重要基础。没有他们的贡献，本项目无法顺利实现。
