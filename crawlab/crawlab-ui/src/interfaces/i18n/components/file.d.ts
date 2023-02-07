interface LComponentsFile {
  editor: {
    navTabs: {
      close: string;
      closeOthers: string;
      closeAll: string;
      showMore: string;
    };
    navMenu: {
      newFile: string;
      newDirectory: string;
      rename: string;
      duplicate: string;
      delete: string;
    };
    sidebar: {
      search: {
        placeholder: string;
      };
      settings: string;
      toggle: {
        showFiles: string;
        hideFiles: string;
      };
    };
    empty: {
      placeholder: string;
    };
    messageBox: {
      prompt: {
        newFile: string;
        newDirectory: string;
        rename: string;
        duplicate: string;
      };
      validator: {
        errorMessage: {
          newNameNotSameAsOldName: string;
        };
      };
    };
    settings: {
      title: string;
      tabs: {
        general: string;
        edit: string;
        indentation: string;
        cursor: string;
      };
      form: {
        title: {
          theme: string;
          indentUnit: string;
          smartIndent: string;
          tabSize: string;
          indentWithTabs: string;
          electricChars: string;
          keyMap: string;
          lineWrapping: string;
          lineNumbers: string;
          showCursorWhenSelecting: string;
          lineWiseCopyCut: string;
          pasteLinesPerSelection: string;
          undoDepth: string;
          cursorBlinkRate: string;
          cursorScrollMargin: string;
          cursorHeight: string;
          maxHighlightLength: string;
          spellcheck: string;
          autocorrect: string;
          autocapitalize: string;
          highlightSelectionMatches: string;
          matchBrackets: string;
          matchTags: string;
          autoCloseBrackets: string;
          autoCloseTags: string;
          showHint: string;
        };
        description: {
          theme: string;
          indentUnit: string;
          smartIndent: string;
          tabSize: string;
          indentWithTabs: string;
          electricChars: string;
          keyMap: string;
          lineWrapping: string;
          lineNumbers: string;
          showCursorWhenSelecting: string;
          lineWiseCopyCut: string;
          pasteLinesPerSelection: string;
          undoDepth: string;
          cursorBlinkRate: string;
          cursorScrollMargin: string;
          cursorHeight: string;
          maxHighlightLength: string;
          spellcheck: string;
          autocorrect: string;
          autocapitalize: string;
          highlightSelectionMatches: string;
          matchBrackets: string;
          matchTags: string;
          autoCloseBrackets: string;
          autoCloseTags: string;
          showHint: string;
        };
      }
    };
  };
  upload: {
    title: string;
    buttons: {
      files: {
        dragFilesHereOr: string;
        clickToUpload: string;
      };
      folder: {
        clickToSelectFolderToUpload: string;
      };
    };
    tooltip: {
      folderName: string;
      filesCount: string;
    };
    mode: {
      folder: string;
      files: string;
    };
    fileList: {
      title: string;
    };
  };
}
