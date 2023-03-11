/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

export const DIFF_EDITOR_OPTIPONS: any = {
  theme: 'arcticSql',
  language: 'sql',
  fontSize: 12,
  lineHeight: 24,
  fontFamily: 'Monaco, Menlo, "Ubuntu Mono", Consolas, source-code-pro, monospace',
  folding: true,
  suggestLineHeight: 20,
  autoIndent: true,
  renderLineHighlight: 'all',
  scrollBeyondLastLine: false,
  contextmenu: false,
  readOnly: true,
  fixedOverflowWidgets: true
}

export const EDITOR_OPTIONS = Object.assign({}, DIFF_EDITOR_OPTIPONS, {
  theme: 'arcticSql',
  language: 'sql',
  readOnly: false,
  lineHeight: 24,
  fontSize: 12,
  fontFamily: 'Monaco, Menlo, "Ubuntu Mono", Consolas, source-code-pro, monospace',
  lineNumbersMinChars: 3,
  wordWrap: 'on',
  renderLineHighlight: 'all',
  minimap: {
    enabled: false
  },
  contextmenu: false,
  automaticLayout: true,
  scrollBeyondLastLine: false
  // rulers: [100]
})
