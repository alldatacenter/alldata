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

export const Viz = createGlobalStyle`
  /* 覆盖antd 默认样式 */
  @media (max-width: 575px) {
    .datart-viz .ant-form .ant-form-item .ant-form-item-label,
    .datart-viz .ant-form .ant-form-item .ant-form-item-control {
      flex: 1;
    }
  }
`;
