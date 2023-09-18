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

import { darken, getLuminance, lighten } from 'polished';
import React, { useContext } from 'react';
import styled from 'styled-components/macro';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import {
  getWidgetBaseStyle,
  getWidgetTitle,
} from '../../WidgetManager/utils/utils';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';

export const ResetBtnWidgetCore: React.FC<{}> = () => {
  const widget = useContext(WidgetContext);
  const { onWidgetsReset } = useContext(WidgetActionContext);

  const onQuery = e => {
    e.stopPropagation();
    onWidgetsReset();
  };

  const title = getWidgetTitle(widget.config.customConfig.props);
  title.title = widget.config.name;
  const { background } = getWidgetBaseStyle(widget.config.customConfig.props);
  return (
    <Wrapper color={background.color} onClick={onQuery}>
      <span
        style={{
          color: title.font.color,
          fontSize: title.font.fontSize,
          fontWeight: title.font.fontWeight,
          fontFamily: title.font.fontFamily,
        }}
      >
        {title.title}
      </span>
    </Wrapper>
  );
};

const Wrapper = styled.div<{ color: string }>`
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;

  cursor: pointer;
  &:hover {
    background: ${p =>
      getLuminance(p.color) > 0.5
        ? darken(0.05, p.color)
        : lighten(0.05, p.color)};
  }
`;
