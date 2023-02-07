const file: LComponentsFile = {
  editor: {
    navTabs: {
      close: '关闭',
      closeOthers: '关闭其他',
      closeAll: '关闭所有',
      showMore: '展示更多',
    },
    navMenu: {
      newFile: '新建文件',
      newDirectory: '新建目录',
      rename: '重命名',
      duplicate: '复制',
      delete: '删除',
    },
    sidebar: {
      search: {
        placeholder: '搜索文件...',
      },
      settings: '设置',
      toggle: {
        showFiles: '展示文件',
        hideFiles: '隐藏文件',
      },
    },
    empty: {
      placeholder: '您可以通过双击左侧文件来编辑或查看文件.'
    },
    messageBox: {
      prompt: {
        newFile: '请输入新建文件名',
        newDirectory: '请输入新建目录名',
        rename: '请输入新名称',
        duplicate: '请输入新名称',
      },
      validator: {
        errorMessage: {
          newNameNotSameAsOldName: '新名称不能跟旧名称相同',
        },
      }
    },
    settings: {
      title: '文件编辑器设置',
      tabs: {
        general: '通用',
        edit: '编辑',
        indentation: '缩进',
        cursor: '光标',
      },
      form: {
        title: {
          theme: '主题',
          indentUnit: '缩进单位',
          smartIndent: '智能缩进',
          tabSize: '制表符大小',
          indentWithTabs: '用制表符缩进',
          electricChars: '电子字符',
          keyMap: '键位',
          lineWrapping: '自动换行',
          lineNumbers: '行号',
          showCursorWhenSelecting: '选择时显示光标',
          lineWiseCopyCut: '行复制剪切',
          pasteLinesPerSelection: '按选择复制行',
          undoDepth: '撤销深度',
          cursorBlinkRate: '光标闪烁速率',
          cursorScrollMargin: '光标滚动边距',
          cursorHeight: '光标高度',
          maxHighlightLength: '最大高亮长度',
          spellcheck: '拼写检查',
          autocorrect: '自动校正',
          autocapitalize: '自动首字母大写',
          highlightSelectionMatches: '突出显示选择匹配项',
          matchBrackets: '匹配括号',
          matchTags: '匹配标签',
          autoCloseBrackets: '自动关闭括号',
          autoCloseTags: '自动关闭标签',
          showHint: '显示提示',
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
          autoCloseBrackets: 'Will auto-close brackets and quotes when typed. It\'ll auto-close brackets\'\'"".',
          autoCloseTags: 'Will auto-close XML tags when \'>\' or \'/\' is typed.',
          showHint: 'Show Hint',
        },
      },
    },
  },
  upload: {
    title: '文件上传',
    buttons: {
      files: {
        dragFilesHereOr: '拖拽文件至此，或',
        clickToUpload: '点击上传',
      },
      folder: {
        clickToSelectFolderToUpload: '点击选择目录上传',
      }
    },
    tooltip: {
      folderName: '目录名称',
      filesCount: '文件数',
    },
    mode: {
      folder: '目录',
      files: '文件',
    },
    fileList: {
      title: '带上传文件',
    },
  }
};

export default file;
