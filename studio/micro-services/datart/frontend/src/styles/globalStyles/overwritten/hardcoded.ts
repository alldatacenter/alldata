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
import { FONT_SIZE_LABEL, SPACE_SM } from 'styles/StyleConstants';

export const Hardcoded = createGlobalStyle`
  body {
    .ant-form-item-label > label {
      color: ${p => p.theme.textColorLight};
    }

    .ant-form-item-label-left > label {
      padding-left: ${SPACE_SM};

      &:before {
        position: absolute;
        left: 0;
      }
    }

    .ant-popover-inner {
      box-shadow: ${p => p.theme.shadow3};
    }
    .ant-popover.ant-popconfirm {
      z-index: 1060;
    }

    /* fix antd bugs #32919 */
    .ant-tabs-dropdown-menu-item {
      display: flex;
      align-items: center;

      > span {
        flex: 1;
        white-space: nowrap;
      }

      &-remove {
        flex: none;
        margin-left: ${SPACE_SM};
        font-size: ${FONT_SIZE_LABEL};
        color: ${p => p.theme.textColorLight};
        cursor: pointer;
        background: 0 0;
        border: 0;

        &:hover {
          color: ${p => p.theme.primary};
        }
      }
    }
  }

  /* react-grid-layout */
  .react-grid-item.react-grid-placeholder {
    background-color: ${p => p.theme.textColorDisabled} !important;
  }
`;
