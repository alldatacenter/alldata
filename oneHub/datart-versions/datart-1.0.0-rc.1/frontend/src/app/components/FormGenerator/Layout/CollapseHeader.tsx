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

import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_FAMILY,
  FONT_SIZE_BODY,
  FONT_WEIGHT_REGULAR,
} from 'styles/StyleConstants';

const CollapseHeader: FC<{ title: string }> = memo(({ title }) => {
  return <StyledCollapseHeader>{title}</StyledCollapseHeader>;
});

export default CollapseHeader;

const StyledCollapseHeader = styled.span`
  font-family: ${FONT_FAMILY};
  font-size: ${FONT_SIZE_BODY};
  font-weight: ${FONT_WEIGHT_REGULAR};
  color: ${p => p.theme.textColorSnd};
  user-select: none;
`;
