/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// import JSONFormatter from 'json-formatter-js';
// import {languages} from 'monaco-editor/esm/vs/language/json/fillers/monaco-editor-core.js';
import debounce from 'lodash/debounce';
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution';
import { FC, memo, useCallback, useMemo } from 'react';
import MonacoEditor, { monaco } from 'react-monaco-editor';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { FONT_SIZE_BASE } from 'styles/StyleConstants';
import { selectThemeKey } from 'styles/theme/slice/selectors';
export const MockDataEditor: FC<{ originalData: object; onDataChange: any }> =
  memo(({ originalData, onDataChange }) => {
    const theme = useSelector(selectThemeKey);
    //   const formatter = new JSONFormatter(jsonVal);
    const editorValue = JSON.stringify(originalData, null, 4);

    const editorWillMount = useCallback(editor => {}, []);

    const editorDidMount = useCallback(
      (editor: monaco.editor.IStandaloneCodeEditor) => {
        // Removing the tooltip on the read-only editor
        // https://github.com/microsoft/monaco-editor/issues/1742

        editor.getAction('editor.action.formatDocument').run(); //格式化
        editor.setValue(editor.getValue());
        editor.focus();
      },
      [],
    );
    const debouncedEditorChange = useMemo(() => {
      const editorChange = val => {
        try {
          let nextVal = JSON.parse(val);
          onDataChange(nextVal);
        } catch (error) {
          console.warn('error on', error);
        }
      };
      return debounce(editorChange, 500);
    }, [onDataChange]);
    return (
      <StyledWrapper>
        <MonacoEditor
          value={editorValue}
          language="javascript"
          theme={`vs-${theme}`}
          options={{
            selectOnLineNumbers: true,
            automaticLayout: true,
            wordWrap: 'wordWrapColumn',
            wrappingStrategy: 'simple',
            wordWrapBreakBeforeCharacters: ',',
            wordWrapBreakAfterCharacters: ',',
            // disableLayerHinting: true,
            fontSize: FONT_SIZE_BASE * 0.875,
            minimap: { enabled: true },
            readOnly: false,
          }}
          onChange={debouncedEditorChange}
          editorWillMount={editorWillMount}
          editorDidMount={editorDidMount}
        />
      </StyledWrapper>
    );
  });
const StyledWrapper = styled.div`
  flex: 1;
  height: 100%;
`;
