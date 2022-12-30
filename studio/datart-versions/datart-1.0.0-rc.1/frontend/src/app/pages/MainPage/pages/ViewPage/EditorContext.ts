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

import {
  createContext,
  MutableRefObject,
  useCallback,
  useMemo,
  useRef,
  useState,
} from 'react';
import { monaco } from 'react-monaco-editor';

interface EditorContextValue {
  editorInstance: monaco.editor.IStandaloneCodeEditor | undefined;
  editorCompletionItemProviderRef:
    | MutableRefObject<monaco.IDisposable | undefined>
    | undefined;
  setEditor: (editor: monaco.editor.IStandaloneCodeEditor | undefined) => void;
  onRun: () => void;
  onSave: () => void;
  initActions: ({
    onRun,
    onSave,
  }: {
    onRun: () => void;
    onSave: () => void;
  }) => void;
}

const editorContextValue: EditorContextValue = {
  editorInstance: void 0,
  editorCompletionItemProviderRef: void 0,
  setEditor: () => {},
  onRun: () => {},
  onSave: () => {},
  initActions: () => {},
};

export const EditorContext = createContext(editorContextValue);

export const useEditorContext = (): EditorContextValue => {
  const [editorInstance, setEditor] =
    useState<monaco.editor.IStandaloneCodeEditor>();
  const [onRun, setOnRun] = useState(() => () => {});
  const [onSave, setOnSave] = useState(() => () => {});
  const editorCompletionItemProviderRef = useRef<monaco.IDisposable>();

  const initActions = useCallback(({ onRun, onSave }) => {
    setOnRun(() => onRun);
    setOnSave(() => onSave);
  }, []);

  return useMemo(
    () => ({
      editorInstance,
      editorCompletionItemProviderRef,
      setEditor,
      onRun,
      onSave,
      initActions,
    }),
    [
      editorInstance,
      editorCompletionItemProviderRef,
      onRun,
      onSave,
      initActions,
    ],
  );
};
