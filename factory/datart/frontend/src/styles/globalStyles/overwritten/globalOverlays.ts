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
import {
  LEVEL_1000,
  SPACE_SM,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';

export const GlobalOverlays = createGlobalStyle`
  /* app/components/Popup */
  .datart-popup {
    z-index: ${LEVEL_1000 - 1};

    &.on-modal {
      z-index: ${LEVEL_1000 + 30};
    }

    .ant-popover-arrow {
      display: none;
    }
    .ant-popover-inner-content {
      padding: 0;
    }
    .ant-dropdown-menu {
      box-shadow: none;
    }
    &.ant-popover-placement-bottom,
    &.ant-popover-placement-bottomLeft,
    &.ant-popover-placement-bottomRight {
      padding-top: 0;
    }
  }
  

  /* schema table header action dropdown menu */
  .datart-schema-table-header-menu {
    min-width: ${SPACE_TIMES(40)};

    .ant-dropdown-menu-submenu-selected {
      .ant-dropdown-menu-submenu-title {
        color: ${p => p.theme.textColor};
      }
    }
  }

  /* config panel */
  .datart-config-panel {
    &.ant-collapse >
    .ant-collapse-item >
    .ant-collapse-header {
      padding: ${SPACE_XS} 0;
      color: ${p => p.theme.textColor};

      .ant-collapse-arrow {
        margin-right: ${SPACE_XS};
      }
    }

    .ant-collapse-content >
    .ant-collapse-content-box {
      padding: ${SPACE_XS} 0 ${SPACE_SM} !important;
    }
  }

  /* data config section dropdown */
  .datart-data-section-dropdown {
    z-index: ${LEVEL_1000 - 1};
  }

  /* color popover */
  .datart-aggregation-colorpopover{
    .ant-popover-arrow{
      display:none;
    }
  }
`;
