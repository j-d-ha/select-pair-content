# Select Pair Content

![Build](https://github.com/j-d-ha/select-pair-content/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## About The Project

<!-- Plugin description -->
Select Pair Content is a JetBrains IDEA plugin that allows you to quickly select content between
matching pairs like parentheses `()`,
brackets `[]`, braces `{}`, quotes `''` `""` `` ` ` ``, and angle brackets `<>`.

The plugin offers two main operations:

- **Expand Selection** - Select content between the nearest enclosing pair (can be run repeatedly to
  expand further)
- **Shrink Selection** - Undo the previous pair selection, returning to the previous selection
  state

This makes it easier to manipulate code blocks, string contents, and other paired structures without
manually positioning your cursor.

### Keyboard Shortcuts

- `Alt+A` (Windows/Linux) or `Option+A` (Mac): Select content between matching pairs (expand) -
  press repeatedly to expand to outer pairs
- `Shift+Alt+A` (Windows/Linux) or `Shift+Option+A` (Mac): Reverse the previous pair selection

<!-- Plugin description end -->

## Features

- Smart selection of content between matching character pairs:
    - Parentheses: `(content)`
    - Brackets: `[content]`
    - Braces: `{content}`
    - Single quotes: `'content'`
    - Double quotes: `"content"`
    - Backticks: `` `content` ``
    - Angle brackets: `<content>`
- Two selection modes:
    - Expand to include content between matching pairs (can be run repeatedly to expand to outer
      pairs)
    - Shrink to undo the previous pair selection
- Works with nested pairs

## Usage

### Examples

#### Expanding Selection

1. Place your cursor inside a pair of characters, e.g., inside `(some content)`
2. Press **Alt+A** (Windows/Linux) or **Option+A** (Mac) to select the content between the
   parentheses: `some content`
3. Press **Alt+A** (Windows/Linux) or **Option+A** (Mac) again to select the entire expression
   including the parentheses: `(some content)`
4. For nested structures like `function((arg1), arg2)`, you can continue pressing **Alt+A** (
   Windows/Linux) or **Option+A** (Mac) to progressively expand to outer pairs

#### Reversing Selection

1. First use **Alt+A** (Windows/Linux) or **Option+A** (Mac) to select content between matching
   pairs
2. Press **Shift+Alt+A** (Windows/Linux) or **Shift+Option+A** (Mac) to reverse the selection and
   return to the previous state
3. If you've expanded the selection multiple times, you can continue pressing **Shift+Alt+A** (
   Windows/Linux) or **Shift+Option+A** (Mac) to step back through your selection history

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  Select Pair Content"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it
  by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download
  the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains
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
