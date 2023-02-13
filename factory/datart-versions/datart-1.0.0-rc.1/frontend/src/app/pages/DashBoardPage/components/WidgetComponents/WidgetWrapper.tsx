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
import {
  BackgroundConfig,
  BorderConfig,
  WidgetPadding,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { memo } from 'react';
import styled from 'styled-components/macro';
import { getWidgetSomeStyle } from '../../utils/widget';

export const WidgetWrapper: React.FC<{
  background: BackgroundConfig;
  padding: WidgetPadding;
  border: BorderConfig;
}> = memo(props => {
  const { children, ...rest } = props;
  const style = getWidgetSomeStyle(rest);
  return <Wrapper style={style}>{children}</Wrapper>;
});
const Wrapper = styled.div`
  display: flex;
  flex: 1;
  min-height: 0;

  &:hover .widget-tool-dropdown {
    visibility: visible;
  }
`;
