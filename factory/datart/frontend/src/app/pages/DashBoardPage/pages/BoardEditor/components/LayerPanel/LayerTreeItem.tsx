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
import { MoreOutlined } from '@ant-design/icons';
import { TreeDataNode } from 'antd';
import { WidgetDndHandleMask } from 'app/pages/DashBoardPage/components/WidgetComponents/WidgetDndHandleMask';
import { WidgetDropdownList } from 'app/pages/DashBoardPage/components/WidgetComponents/WidgetDropdownList';
import { widgetManagerInstance } from 'app/pages/DashBoardPage/components/WidgetManager/WidgetManager';
import { WidgetContext } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetProvider';
import { WidgetWrapProvider } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetWrapProvider';
import { FC, memo, useContext } from 'react';
import styled from 'styled-components/macro';
import { stopPPG } from 'utils/utils';

export interface LayerNode extends TreeDataNode {
  key: string;
  parentId: string;
  selected: boolean;
  children: LayerNode[];
  content: any;
  widgetIndex: number;
  originalType: string;
  boardId: string;
}

export type EventLayerNode = LayerNode & {
  dragOver: boolean;
  dragOverGapTop: boolean;
  dragOverGapBottom: boolean;
};

export const LayerTreeItem: FC<{ node: LayerNode }> = memo(({ node }) => {
  return (
    <WidgetWrapProvider
      id={node.key}
      key={node.key}
      boardEditing={true}
      boardId={node.boardId}
    >
      <TreeItem node={node} />
    </WidgetWrapProvider>
  );
});

export const TreeItem: FC<{ node: LayerNode }> = memo(({ node }) => {
  const { title } = node;
  const widget = useContext(WidgetContext);
  const canWrapped = widgetManagerInstance.meta(
    widget.config.originalType,
  ).canWrapped;

  return (
    <Item>
      <TitleBlock>
        <h4 title={title as string}>{String(title) || 'untitled-widget'}</h4>
        <WidgetDndHandleMask widgetId={widget?.id} canWrapped={canWrapped} />
      </TitleBlock>
      <WidgetDropdownList
        widget={widget}
        buttonProps={{
          size: 'small',
          icon: <MoreOutlined />,
          onClick: stopPPG,
        }}
      />
    </Item>
  );
});

const Item = styled.div`
  display: flex;
  flex: 1;
  align-items: center;

  h4 {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .widget-tool-dropdown {
    display: none;
  }

  &:hover {
    .widget-tool-dropdown {
      display: block;
    }
  }
`;

const TitleBlock = styled.div`
  position: relative;
  width: calc(100% - 24px);
`;
