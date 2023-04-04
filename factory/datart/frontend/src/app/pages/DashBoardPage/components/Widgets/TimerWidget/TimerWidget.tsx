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
import { Space } from 'antd';
import { WidgetContext } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetProvider';
import { memo, useContext } from 'react';
import { BoardContext } from '../../BoardProvider/BoardProvider';
import { FlexStyle, ZIndexStyle } from '../../WidgetComponents/constants';
import { EditMask } from '../../WidgetComponents/EditMask';
import { LockIconFn } from '../../WidgetComponents/StatusIcon';
import { StyledWidgetToolBar } from '../../WidgetComponents/StyledWidgetToolBar';
import { WidgetDropdownList } from '../../WidgetComponents/WidgetDropdownList';
import { WidgetTitle } from '../../WidgetComponents/WidgetTitle';
import { WidgetWrapper } from '../../WidgetComponents/WidgetWrapper';
import {
  getWidgetBaseStyle,
  getWidgetTitle,
} from '../../WidgetManager/utils/utils';
import { TimerWidgetCore } from './TimerWidgetCore';

export const TimerWidget: React.FC<{ hideTitle: boolean }> = memo(
  ({ hideTitle }) => {
    const widget = useContext(WidgetContext);
    const { editing } = useContext(BoardContext);
    const { background, border, padding } = getWidgetBaseStyle(
      widget.config.customConfig.props,
    );
    const title = getWidgetTitle(widget.config.customConfig.props);
    title.title = widget.config.name;
    return (
      <WidgetWrapper background={background} border={border} padding={padding}>
        <div style={ZIndexStyle}>
          {hideTitle ? null : <WidgetTitle title={title} />}
          <div style={FlexStyle}>
            <TimerWidgetCore />
          </div>
        </div>
        {editing && <EditMask />}
        <StyledWidgetToolBar>
          <Space size={0}>
            <LockIconFn
              boardEditing={editing}
              wid={widget.id}
              lock={widget.config?.lock}
            />
            <WidgetDropdownList widget={widget} />
          </Space>
        </StyledWidgetToolBar>
      </WidgetWrapper>
    );
  },
);
