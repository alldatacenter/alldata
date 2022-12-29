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

import { WidgetContext } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetProvider';
import { editBoardStackActions } from 'app/pages/DashBoardPage/pages/BoardEditor/slice';
import { memo, useContext, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import { AutoGroupWidget } from './AutoGroupWidget';
import { FreeGroupWidget } from './FreeGroupWidget';

export const GroupWidget1: React.FC<{}> = memo(() => {
  const widget = useContext(WidgetContext);
  const { onEditDeleteActiveWidgets } = useContext(WidgetActionContext);
  const boardType = widget.config.boardType;
  // Only board type is auto and no parent id use auto board widget, otherwise is free board widget.
  const isAutoGroupWidget = boardType === 'auto' && !widget.parentId;
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(
      editBoardStackActions.adjustGroupWidgets({
        groupIds: [widget.id],
        isAutoGroupWidget,
      }),
    );
    if (!widget.config.children?.length) {
      onEditDeleteActiveWidgets([widget.id]);
    }
  }, [
    dispatch,
    isAutoGroupWidget,
    onEditDeleteActiveWidgets,
    widget.config.children?.length,
    widget.config?.name,
    widget.id,
  ]);

  return isAutoGroupWidget ? <AutoGroupWidget /> : <FreeGroupWidget />;
});
