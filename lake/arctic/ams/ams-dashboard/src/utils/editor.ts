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

/**
 * Keywords and formatting configuration in sql editor
 */
import * as monaco from 'monaco-editor'
import { language as sqlLanguage } from './sql'

import sqlFormatter from 'sql-formatter'

const registerSql = () => {
  // SQL keyword hints
  monaco.languages.registerCompletionItemProvider('sql', {
    provideCompletionItems: (model, position, context, token) => {
      const textUntilPosition = model.getValueInRange({
        startLineNumber: position.lineNumber,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column
      })
      const match = textUntilPosition.match(/(\S+)$/)
      const suggestions: monaco.languages.CompletionItem[] = []
      if (match) {
        const matchStr = match[0].toUpperCase()
        sqlLanguage.keywords.forEach((item: string) => {
          if (item.startsWith(matchStr)) {
            suggestions.push({
              label: item,
              kind: monaco.languages.CompletionItemKind.Keyword,
              insertText: item
            } as monaco.languages.CompletionItem)
          }
        })
        sqlLanguage.operators.forEach((item: string) => {
          if (item.startsWith(matchStr)) {
            suggestions.push({
              label: item,
              kind: monaco.languages.CompletionItemKind.Operator,
              insertText: item
            } as monaco.languages.CompletionItem)
          }
        })
        sqlLanguage.builtinFunctions.forEach((item: string) => {
          if (item.startsWith(matchStr)) {
            suggestions.push({
              label: item,
              kind: monaco.languages.CompletionItemKind.Function,
              insertText: item
            } as monaco.languages.CompletionItem)
          }
        })
      }
      return {
        suggestions: Array.from(new Set(suggestions))
      }
    }
  })

  // format SQL
  monaco.languages.registerDocumentFormattingEditProvider('sql', {
    provideDocumentFormattingEdits(model) {
      const formatted = sqlFormatter.format(model.getValue())
      return [{
        range: model.getFullModelRange(),
        text: formatted.replace(/\s-\s/g, '-')
      }]
    }
  })

  const themeData: any = {
    base: 'vs',
    inherit: !1,
    colors: {
      'editorHoverWidget.background': '#FAFAFA',
      'editorHoverWidget.border': '#DEDEDE',
      'editor.lineHighlightBackground': '#EFF8FF',
      'editor.selectionBackground': '#D5D5EF',
      'editorLineNumber.foreground': '#999999',
      'editorSuggestWidget.background': '#FFFFFF',
      'editorSuggestWidget.selectedBackground': '#EFF8FF'
    },
    rules: [{
      token: 'comment',
      foreground: '8E908C'
    }, {
      token: 'comments',
      foreground: '8E908C'
    }, {
      token: 'keyword',
      foreground: '8959A8'
    }, {
      token: 'predefined',
      foreground: '11B7BE'
    }, {
      token: 'doubleString',
      foreground: 'AB1010'
    }, {
      token: 'singleString',
      foreground: 'AB1010'
    }, {
      token: 'number',
      foreground: 'AB1010'
    }, {
      token: 'string.sql',
      foreground: '718C00'
    }]
  }
  monaco.editor.defineTheme('arcticSql', themeData)
}

const registerLogLanguage = () => {
  monaco.languages.register({ id: 'logLanguage' })

  monaco.languages.setMonarchTokensProvider('logLanguage', {
    tokenizer: {
      root: [
        [/INFO.*/, 'custom-info'],
        [/ERROR.*/, 'custom-error'],
        [/WARN.*/, 'custom-warn'],
        [/DEBUG.*/, 'custom-debug'],
        [/\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2},\d{3}/, 'custom-date']
      ]
    }
  })

  const themeData: monaco.editor.IStandaloneThemeData = {
    base: 'vs',
    inherit: false,
    colors: {
      'editor.background': '#f6f7f8'
    },
    rules: [
      { token: 'custom-info', foreground: '808080' },
      { token: 'custom-error', foreground: 'ff0000', fontStyle: 'bold' },
      { token: 'custom-warn', foreground: 'ffa500' },
      { token: 'custom-debug', foreground: 'ffa500' },
      { token: 'custom-date', foreground: '008800' },
      { token: '', background: '#f6f7f8' }
    ]
  }
  monaco.editor.defineTheme('logTheme', themeData)
}

registerSql()
registerLogLanguage()
