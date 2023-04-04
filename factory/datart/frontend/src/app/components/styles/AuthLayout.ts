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

import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_BASE,
  FONT_SIZE_HEADING,
  FONT_SIZE_ICON_XL,
  FONT_WEIGHT_MEDIUM,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XL,
  SPACE_XS,
} from 'styles/StyleConstants';

export const Form = styled.div`
  width: ${SPACE_TIMES(100)};
  padding: ${SPACE_XL} ${SPACE_XL} ${SPACE_TIMES(8)};
  margin-top: ${SPACE_TIMES(10)};
  background-color: ${p => p.theme.componentBackground};
  border-radius: ${BORDER_RADIUS};
  box-shadow: ${p => p.theme.shadowSider};

  .ant-form-item {
    margin-bottom: ${SPACE_XL};

    &.last {
      margin-bottom: 0;
    }
  }

  .ant-form-item-with-help {
    margin-bottom: ${SPACE_XS};
  }

  .ant-input,
  .ant-input-affix-wrapper {
    color: ${p => p.theme.textColorSnd};
    background-color: ${p => p.theme.bodyBackground};
    border-color: ${p => p.theme.bodyBackground};

    &:hover,
    &:focus {
      border-color: ${p => p.theme.bodyBackground};
      box-shadow: none;
    }

    &:focus {
      background-color: ${p => p.theme.emphasisBackground};
    }
  }

  .ant-input-affix-wrapper:focus,
  .ant-input-affix-wrapper-focused {
    background-color: ${p => p.theme.emphasisBackground};
    box-shadow: none;
  }
`;

export const Picture = styled.div`
  display: flex;
  align-items: center;
  height: ${SPACE_TIMES(24)};
  margin-top: ${SPACE_XL};
  font-size: ${FONT_SIZE_BASE * 4}px;
  color: ${p => p.color || p.theme.normal};
`;

export const Title = styled.h1`
  margin: ${SPACE_XL} 0 ${SPACE_MD};
  font-size: ${FONT_SIZE_ICON_XL};
  font-weight: ${FONT_WEIGHT_MEDIUM};
  color: ${p => p.theme.textColorSnd};
`;

export const Description = styled.p`
  font-size: ${FONT_SIZE_HEADING};
  color: ${p => p.theme.textColorLight};

  .btn {
    font-size: ${FONT_SIZE_HEADING};
  }
`;
