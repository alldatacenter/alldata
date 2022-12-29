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

import { createGlobalStyle } from 'styled-components/macro';
import { FONT_FAMILY, FONT_SIZE_BODY } from 'styles/StyleConstants';

/* istanbul ignore next */
export const Base = createGlobalStyle`
  body {
    font-family: ${FONT_FAMILY};
    font-size: ${FONT_SIZE_BODY};
    background-color: ${p => p.theme.bodyBackground};

    h1,h2,h3,h4,h5,h6 {
      margin: 0;
      font-weight: inherit;
      color: inherit;
    }

    p, figure {
      margin: 0;
    }

    ul {
      padding: 0;
      margin: 0;
    }

    li {
      list-style-type: none;
    }

    input {
      padding: 0;
    }

    table th {
      padding: 0;
      text-align: center;
    }

    * {
      -webkit-overflow-scrolling: touch;
    }
  }
`;
