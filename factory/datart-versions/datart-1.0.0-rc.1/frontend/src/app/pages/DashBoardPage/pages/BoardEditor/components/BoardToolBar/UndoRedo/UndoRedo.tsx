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
import { RedoOutlined, UndoOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import { ToolbarButton } from 'app/components';
import { FC, MouseEventHandler, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { selectFutureState, selectPastState } from '../../../slice/selectors';

export const UndoBtn: FC<{
  fn: MouseEventHandler<HTMLElement> | undefined;
  title: string;
}> = ({ fn, title }) => {
  const pastState = useSelector(selectPastState);
  const canUndo = useMemo(() => !!pastState.length, [pastState.length]);
  return (
    <Tooltip title={title}>
      <ToolbarButton disabled={!canUndo} onClick={fn} icon={<UndoOutlined />} />
    </Tooltip>
  );
};
export const RedoBtn: FC<{
  fn: MouseEventHandler<HTMLElement> | undefined;
  title: string;
}> = ({ fn, title }) => {
  const futureState = useSelector(selectFutureState);

  const canRedo = useMemo(() => !!futureState.length, [futureState.length]);
  return (
    <Tooltip title={title}>
      <ToolbarButton disabled={!canRedo} onClick={fn} icon={<RedoOutlined />} />
    </Tooltip>
  );
};
