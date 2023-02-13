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
import { WidgetTitleConfig } from '../../pages/Board/slice/types';

export const WidgetTitle: FC<{
  title: WidgetTitleConfig;
}> = memo(({ title }) => {
  if (!title.showTitle) {
    return null;
  }
  return (
    <StyledWrap conf={title}>
      <NameWrap className="widget-name" conf={title}>
        {title.title}
      </NameWrap>
    </StyledWrap>
  );
});

const StyledWrap = styled.div<{ conf: WidgetTitleConfig }>`
  width: 100%;
  line-height: 24px;
  text-align: ${p => p.conf.textAlign};
  cursor: pointer;
`;
const NameWrap = styled.span<{ conf: WidgetTitleConfig }>`
  font-family: ${p => p.conf.font.fontFamily};
  font-size: ${p => p.conf.font.fontSize}px;
  font-style: ${p => p.conf.font.fontStyle};
  font-weight: ${p => p.conf.font.fontWeight};
  color: ${p => p.conf.font.color};
  text-align: ${p => p.conf.textAlign};
`;
