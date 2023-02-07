const file: LComponentsFile = {
  editor: {
    navTabs: {
      close: 'Close',
      closeOthers: 'Close Others',
      closeAll: 'Close All',
      showMore: 'Show More',
    },
    navMenu: {
      newFile: 'New File',
      newDirectory: 'New Directory',
      rename: 'Rename',
      duplicate: 'Duplicate',
      delete: 'Delete',
    },
    sidebar: {
      search: {
        placeholder: 'Search files...',
      },
      settings: 'Settings',
      toggle: {
        showFiles: 'Show Files',
        hideFiles: 'Hide Files',
      },
    },
    empty: {
      placeholder: 'You can edit or view a file by double-clicking one of the files on the left.'
    },
    messageBox: {
      prompt: {
        newFile: 'Please enter the name of the new file',
        newDirectory: 'Please enter the name of the new directory',
        rename: 'Please enter the new name',
        duplicate: 'Please enter the new name',
      },
      validator: {
        errorMessage: {
          newNameNotSameAsOldName: 'New name cannot be the same as the old name',
        },
      }
    },
    settings: {
      title: 'File Editor Settings',
      tabs: {
        general: 'General',
        edit: 'Edit',
        indentation: 'Indentation',
        cursor: 'Cursor',
      },
      form: {
        title: {
          theme: 'Theme',
          indentUnit: 'Indent Unit',
          smartIndent: 'Smart Indent',
          tabSize: 'Tab Size',
          indentWithTabs: 'Indent with Tabs',
          electricChars: 'Electric Chars',
          keyMap: 'Keymap',
          lineWrapping: 'Line Wrapping',
          lineNumbers: 'Line Numbers',
          showCursorWhenSelecting: 'Show Cursor When Selecting',
          lineWiseCopyCut: 'Line-wise Copy-Cut',
          pasteLinesPerSelection: 'Paste Lines per Selection',
          undoDepth: 'Undo Depth',
          cursorBlinkRate: 'Cursor Blink Rate',
          cursorScrollMargin: 'Cursor Scroll Margin',
          cursorHeight: 'Cursor Height',
          maxHighlightLength: 'Max Highlight Length',
          spellcheck: 'Spell Check',
          autocorrect: 'Auto Correct',
          autocapitalize: 'Auto Capitalize',
          highlightSelectionMatches: 'Highlight Selection Matches',
          matchBrackets: 'Match Brackets',
          matchTags: 'Match Tags',
          autoCloseBrackets: 'Auto-Close Brackets',
          autoCloseTags: 'Auto-Close Tags',
          showHint: 'Show Hint',
        },
        description: {
          theme: 'The theme to style the editor with.',
          indentUnit: 'How many spaces a block (whatever that means in the edited language) should be indented.',
          smartIndent: 'Whether to use the context-sensitive indentation that the mode provides (or just indent the same as the line before).',
          tabSize: 'The width of a tab character. Defaults to 4.',
          indentWithTabs: 'Whether, when indenting, the first N*tabSize spaces should be replaced by N tabs.',
          electricChars: 'Configures whether the editor should re-indent the current line when a character is typed that might change its proper indentation (only works if the mode supports indentation).',
          keyMap: 'Configures the keymap to use.',
          lineWrapping: 'Whether to scroll or wrap for long lines.',
          lineNumbers: 'Whether to show line numbers to the left of the editor.',
          showCursorWhenSelecting: 'Whether the cursor should be drawn when a selection is active.',
          lineWiseCopyCut: 'When enabled, doing copy or cut when there is no selection will copy or cut the whole lines that have cursors on them.',
          pasteLinesPerSelection: 'When pasting something from an external source (not from the editor itself), if the number of lines matches the number of selection, the editor will by default insert one line per selection. You can set this to false to disable that behavior.',
          undoDepth: 'The maximum number of undo levels that the editor stores.',
          cursorBlinkRate: 'Half-period in milliseconds used for cursor blinking.',
          cursorScrollMargin: 'How much extra space to always keep above and below the cursor when approaching the top or bottom of the visible view in a scrollable document.',
          cursorHeight: 'Determines the height of the cursor. Setting it to 1, means it spans the whole height of the line. For some fonts (and by some tastes) a smaller height (for example 0.85), which causes the cursor to not reach all the way to the bottom of the line, looks better',
          maxHighlightLength: 'When highlighting long lines, in order to stay responsive, the editor will give up and simply style the rest of the line as plain text when it reaches a certain position.',
          spellcheck: 'Specifies whether or not spellcheck will be enabled on the input.',
          autocorrect: 'Specifies whether or not auto-correct will be enabled on the input.',
          autocapitalize: 'Specifies whether or not auto-capitalization will be enabled on the input.',
          highlightSelectionMatches: 'Adds a highlightSelectionMatches option that can be enabled to highlight all instances of a currently selected word. When enabled, it causes the current word to be highlighted when nothing is selected.',
          matchBrackets: 'When set to true or an options object, causes matching brackets to be highlighted whenever the cursor is next to them.',
          matchTags: 'When enabled will cause the tags around the cursor to be highlighted',
          autoCloseBrackets: 'Will auto-close brackets and quotes when typed. It\'ll auto-close brackets\'"".',
          autoCloseTags: 'Will auto-close XML tags when \'>\' or \'/\' is typed.',
          showHint: 'Show Hint',
        },
      },
    },
  },
  upload: {
    title: 'Files Upload',
    buttons: {
      files: {
        dragFilesHereOr: 'Drag files here, or',
        clickToUpload: 'click to upload',
      },
      folder: {
        clickToSelectFolderToUpload: 'Click to select folder to upload',
      }
    },
    tooltip: {
      folderName: 'Folder Name',
      filesCount: 'Files Count',
    },
    mode: {
      folder: 'Folder',
      files: 'Files',
    },
    fileList: {
      title: 'Files to Upload',
    },
  }
};

export default file;
