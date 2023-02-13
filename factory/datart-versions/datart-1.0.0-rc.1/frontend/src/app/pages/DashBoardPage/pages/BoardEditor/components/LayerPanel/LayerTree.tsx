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

import { Tree } from 'app/components';
import { renderIcon } from 'app/hooks/useGetVizIcon';
import { WidgetActionContext } from 'app/pages/DashBoardPage/components/ActionProvider/WidgetActionProvider';
import widgetManager from 'app/pages/DashBoardPage/components/WidgetManager';
import { FC, memo, useCallback, useContext } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { isEmptyArray } from 'utils/object';
import { stopPPG } from 'utils/utils';
import { dropLayerNodeAction } from '../../slice/actions/actions';
import {
  selectEditingWidgetIds,
  selectLayerTree,
  selectSelectedIds,
} from '../../slice/selectors';
import { LayerTreeItem } from './LayerTreeItem';

export const LayerTree: FC<{}> = memo(() => {
  const dispatch = useDispatch();
  const treeData = useSelector(selectLayerTree);
  const renderTreeItem = useCallback(n => <LayerTreeItem node={n} />, []);
  const { onEditSelectWidget } = useContext(WidgetActionContext);
  const editingWidgetIds = useSelector(selectEditingWidgetIds);
  const selectedKeys = useSelector(selectSelectedIds);

  const treeSelect = useCallback(
    (_, { node, nativeEvent }) => {
      onEditSelectWidget({
        multipleKey: nativeEvent.shiftKey,
        id: node.key as string,
        selected: true,
      });
    },
    [onEditSelectWidget],
  );

  const icon = useCallback(
    node => renderIcon(widgetManager.meta(node.originalType).icon),
    [],
  );

  const onDrop = useCallback(
    info => dispatch(dropLayerNodeAction(info)),
    [dispatch],
  );

  return (
    <Tree
      className="medium"
      draggable={isEmptyArray(editingWidgetIds)}
      multiple
      loading={false}
      titleRender={renderTreeItem}
      icon={icon}
      onSelect={treeSelect}
      onClick={stopPPG}
      onDrop={onDrop}
      treeData={treeData}
      selectedKeys={selectedKeys}
      defaultExpandAll
    />
  );
});
