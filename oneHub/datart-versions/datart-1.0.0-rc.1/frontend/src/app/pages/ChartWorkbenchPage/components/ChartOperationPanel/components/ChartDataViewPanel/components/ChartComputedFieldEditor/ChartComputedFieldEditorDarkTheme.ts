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

// https://getbootstrap.com/docs/4.1/getting-started/theming/#theme-colors
const ChartComputedFieldEditorDarkTheme = {
  base: 'vs-dark',
  inherit: true,
  rules: [
    { token: 'dql-field', foreground: '007bff', fontStyle: 'bold' },
    { token: 'dql-variable', foreground: '28a745', fontStyle: 'bold' },
    { token: 'dql-function', foreground: 'fd7e14' },
    { token: 'dql-operator', foreground: '17a2b8' },
    { token: 'dql-comment', foreground: '6c757d' },
  ],
};

export default ChartComputedFieldEditorDarkTheme;
