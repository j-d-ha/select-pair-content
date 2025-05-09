# Select Pair Content

![Build](https://github.com/j-d-ha/select-pair-content/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/27313.svg)](https://plugins.jetbrains.com/plugin/27313)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/27313.svg)](https://plugins.jetbrains.com/plugin/27313)

## About The Plugin

<!-- Plugin description -->
Select Pair Content is a JetBrains IDE plugin that allows you to quickly select content within a
file. It has two main functions:

- Selecting content between matching pairs of tokens such as parentheses `()`,
  brackets `[]`, braces `{}`, quotes `''` `""` `` ` ` ``, and angle brackets `<>`.
- Select content based on the
  underlying [PSI Elements](https://plugins.jetbrains.com/docs/intellij/psi.html)
  that make up a file.

For both pair and element based selection, the plugin supports expanding a selection and shrinking
it. Repeated invocations of the plugin action will expand out
the given selection. Exact behavior is determined by the given action as follows:

- **Pair Select**
    - **Expand**: Repeated expand action invocations will expand out the selection to the next
      pair of matching tokens.
    - **Shrink**: Repeated shrink action invocations will shrink the selection to the next pair of
      matching
      tokens within the existing selection.
- **Element Select**
    - **Expand**: Repeated expand action invocations will select the given element's parent element.
    - **Shrink**:Repeated shrink action invocations will shrink the selection to the given element's
      child element that contains the caret.

### Keyboard Shortcuts

| Action                        | Windows/Linux | MacOS      |
|-------------------------------|---------------|------------|
| Expand Pair Content Selection | `Alt+Q`       | `Option+Q` |
| Shrink Pair Content Selection | `Alt+A`       | `Option+A` |
| Expand Element Selection      | `Alt+W`       | `Option+W` |
| Shrink Element Selection      | `Alt+S`       | `Option+S` |

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  Select Pair Content"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/27313) and install it
  by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download
  the [latest release](https://plugins.jetbrains.com/plugin/27313/versions) from JetBrains
  Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
  disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/j-d-ha/select-pair-content/releases/latest) and
  install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
  disk...</kbd>

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

This plugin is based on the [code-buddy-plugin](https://github.com/srizzo/code-buddy-plugin#)
plugin.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
