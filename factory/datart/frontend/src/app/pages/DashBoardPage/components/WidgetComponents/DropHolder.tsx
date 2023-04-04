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
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CONTAINER_TAB } from 'app/pages/DashBoardPage/constants';
import { rgba } from 'polished';
import { memo } from 'react';
import { DropTargetMonitor, useDrop } from 'react-dnd';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';
import { ContainerItem } from '../../pages/Board/slice/types';
import { editBoardStackActions } from '../../pages/BoardEditor/slice';
import { DropItem } from './WidgetDndHandleMask';

export interface DropHolderProps {
  tabItem: ContainerItem;
  tabWidgetId: string;
}
export const DropHolder: React.FC<DropHolderProps> = memo(
  ({ tabItem, tabWidgetId }) => {
    const dispatch = useDispatch();
    const t = useI18NPrefix(`viz.widget.tips`);
    const [{ isOver, canDrop }, refDrop] = useDrop(
      () => ({
        accept: CONTAINER_TAB,
        item: { tabItem, parentId: tabWidgetId },
        drop: (dropItem: DropItem) => {
          dispatch(
            editBoardStackActions.addWidgetToTabWidget({
              tabWidgetId,
              tabItem: {
                ...tabItem,
                childWidgetId: dropItem.childId,
              },
              sourceId: dropItem.childId,
            }),
          );
        },
        canDrop: (dropItem: DropItem) => {
          return dropItem.canWrapped;
        },
        collect: (monitor: DropTargetMonitor) => ({
          isOver: monitor.isOver(),
          canDrop: monitor.canDrop(),
        }),
      }),
      [],
    );

    return (
      <DropWrap ref={refDrop} canDrop={canDrop} isOver={isOver}>
        <div className="center">{t('dragChartFromLeftPanel')}</div>
      </DropWrap>
    );
  },
);

const DropWrap = styled.div<{
  bgColor?: string;
  canDrop: boolean;
  isOver: boolean;
}>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background-color: ${props =>
    props.canDrop
      ? rgba(props.theme.success, 0.25)
      : props.isOver
      ? rgba(props.theme.error, 0.25)
      : props.theme.emphasisBackground};

  .center {
    text-align: center;
  }
`;
