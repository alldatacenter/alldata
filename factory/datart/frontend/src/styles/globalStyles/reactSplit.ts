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
import { LEVEL_10, SPACE_TIMES } from 'styles/StyleConstants';

export const ReactSplit = createGlobalStyle`
  /* react-split */
  .datart-split {
    min-width: 0;
    min-height: 0;

    .gutter-horizontal {
      &:before {
        width: 2px;
        height: 100%;
        transform: translate(-50%, 0);
      }
      &:after {
        width: ${SPACE_TIMES(2)};
        height: 100%;
        cursor: ew-resize;
        transform: translate(-50%, 0);
      }
    }

    .gutter-vertical {
      &:before {
        width: 100%;
        height: 2px;
        transform: translate(0, -50%);
      }
      &:after {
        width: 100%;
        height: ${SPACE_TIMES(2)};
        cursor: ns-resize;
        transform: translate(0, -50%);
      }
    }

    .gutter-horizontal,
    .gutter-vertical{
      position: relative;

      &:before {
        position: absolute;
        z-index: ${LEVEL_10};
        content: '';
      }
      &:after {
        position: absolute;
        z-index: ${LEVEL_10};
        content: '';
      }
      &:hover,
      &:active {
        &:before {
          background-color: ${p => p.theme.primary};
        }
      }
    }
  }
`;
