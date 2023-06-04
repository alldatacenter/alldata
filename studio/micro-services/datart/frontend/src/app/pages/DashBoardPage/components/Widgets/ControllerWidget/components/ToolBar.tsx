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
import { FC, memo, useContext } from 'react';
import { BoardContext } from '../../../BoardProvider/BoardProvider';
import {
  ErrorIcon,
  LoadingIcon,
  LockIconFn,
  WaitIconFn,
} from '../../../WidgetComponents/StatusIcon';
import { StyledWidgetToolBar } from '../../../WidgetComponents/StyledWidgetToolBar';
import { WidgetDropdownList } from '../../../WidgetComponents/WidgetDropdownList';
import { WidgetInfoContext } from '../../../WidgetProvider/WidgetInfoProvider';
import { WidgetContext } from '../../../WidgetProvider/WidgetProvider';

export const ToolBar: FC = memo(() => {
  const { editing: boardEditing } = useContext(BoardContext);
  const { loading, rendered, errInfo } = useContext(WidgetInfoContext);
  const widget = useContext(WidgetContext);
  return (
    <StyledWidgetToolBar>
      <Space size={0}>
        <LoadingIcon loading={loading} />
        <WaitIconFn rendered={rendered} widget={widget} />
        <ErrorIcon errInfo={errInfo} />
        <LockIconFn
          boardEditing={boardEditing}
          lock={widget.config.lock}
          wid={widget.id}
        />
        <WidgetDropdownList widget={widget} />
      </Space>
    </StyledWidgetToolBar>
  );
});
