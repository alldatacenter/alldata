/**
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

import { css } from "styled-components";

export const theme = css`
  .cm-s-docz-light.CodeMirror {
    border-radius: 0;
    background: #fbfcfd;
    color: #24292e;
  }

  .cm-s-docz-light .CodeMirror-gutters {
    background: #fbfcfd;
    border-right-width: 0;
    border-radius: 0;
  }

  .cm-s-docz-light .CodeMirror-guttermarker {
    color: white;
  }

  .cm-s-docz-light .CodeMirror-guttermarker-subtle {
    color: #d0d0d0;
  }

  .cm-s-docz-light .CodeMirror-linenumber {
    color: #959da5;
    background: #fbfcfd;
  }

  .cm-s-docz-light .CodeMirror-cursor {
    border-left: 1px solid #24292e;
  }

  .cm-s-docz-light div.CodeMirror-selected,
  .cm-s-docz-light .CodeMirror-line::selection,
  .cm-s-docz-light .CodeMirror-line > span::selection,
  .cm-s-docz-light .CodeMirror-line > span > span::selection,
  .cm-s-docz-light .CodeMirror-line::-moz-selection,
  .cm-s-docz-light .CodeMirror-line > span::-moz-selection,
  .cm-s-docz-light .CodeMirror-line > span > span::-moz-selection {
    background: #c8c8fa;
  }

  .cm-s-docz-light .CodeMirror-activeline-background {
    background: #fafbfc;
  }

  .cm-s-docz-light .CodeMirror-matchingbracket {
    text-decoration: underline;
    color: #949495 !important;
  }

  .cm-s-docz-light .CodeMirror-lines {
    background: #fbfcfd;
  }

  .cm-s-docz-light .cm-comment {
    color: #6a737d;
  }

  .cm-s-docz-light .cm-tag,
  .cm-s-docz-light .cm-bracket {
    color: #d73a49;
  }

  .cm-s-docz-light .cm-constant {
    color: #005cc5;
  }

  .cm-s-docz-light .cm-entity {
    font-weight: normal;
    font-style: normal;
    text-decoration: none;
    color: #6f42c1;
  }

  .cm-s-docz-light .cm-keyword {
    font-weight: normal;
    font-style: normal;
    text-decoration: none;
    color: #d73a49;
  }

  .cm-s-docz-light .cm-storage {
    color: #d73a49;
  }

  .cm-s-docz-light .cm-string {
    font-weight: normal;
    font-style: normal;
    text-decoration: none;
    color: #005cc5;
  }

  .cm-s-docz-light .cm-support {
    font-weight: normal;
    font-style: normal;
    text-decoration: none;
    color: #005cc5;
  }

  .cm-s-docz-light .cm-variable {
    font-weight: normal;
    font-style: normal;
    text-decoration: none;
    color: #e36209;
  }
`;