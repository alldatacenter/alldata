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
import { makeSelectBoardConfigById } from 'app/pages/DashBoardPage/pages/Board/slice/selector';
import {
  BoardState,
  Dashboard,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { XYCoord } from 'dnd-core';
import React, { useEffect, useRef } from 'react';
import { DropTargetMonitor, useDrag, useDrop } from 'react-dnd';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_TITLE,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_BODY,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';
import { storyActions } from '../slice';
import { NameCard } from './PageThumbnailList';

export interface IProps {
  page: NameCard;
  index: number;
  selected: boolean;
  canDrag: boolean;
  moveCard: (dragPageId: string, hoverPageId: string) => void;
  moveEnd: () => void;
}
interface DragItem {
  index: number;
  id: string;
  selected: boolean;
}
const ThumbnailItem: React.FC<IProps> = ({
  page,
  index,
  selected,
  canDrag,
  moveCard,
  moveEnd,
}) => {
  const dispatch = useDispatch();
  const ItemRef = useRef<HTMLDivElement>(null);
  const dashboard = useSelector((state: { board: BoardState }) =>
    makeSelectBoardConfigById()(state, page.relId),
  );
  useEffect(() => {
    try {
      const { thumbnail, name } = dashboard as Dashboard;
      dispatch(
        storyActions.updateStoryPageNameAndThumbnail({
          storyId: page.storyId,
          pageId: page.id,
          name,
          thumbnail,
        }),
      );
    } catch (error) {
      // TODO(Stephen): why the original code eat error, try to remove catch
    }
    // storyActions.updateStoryPageNameAndThumbnail
  }, [dashboard, dispatch, page.id, page.storyId]);
  const [{ handlerId }, drop] = useDrop({
    accept: 'storyBoardListDnd',
    collect(monitor) {
      return {
        handlerId: monitor.getHandlerId(),
      };
    },
    hover(item: DragItem, monitor: DropTargetMonitor) {
      if (!ItemRef.current) {
        return;
      }
      const dragIndex = item.index;
      const hoverIndex = page.index;
      const hoverSelected = item.selected;
      // const dragId = item.id;
      // Don't replace items with themselves
      if (dragIndex === hoverIndex) {
        return;
      }

      // Determine rectangle on screen
      const hoverBoundingRect = ItemRef.current?.getBoundingClientRect();

      // Get vertical middle
      const hoverMiddleY =
        (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;

      // Determine mouse position
      const clientOffset = monitor.getClientOffset();

      // Get pixels to the top
      const hoverClientY = (clientOffset as XYCoord).y - hoverBoundingRect.top;

      // Only perform the move when the mouse has crossed half of the items height
      // When dragging downwards, only move when the cursor is below 50%
      // When dragging upwards, only move when the cursor is above 50%

      // Dragging downwards
      if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
        return;
      }

      // Dragging upwards
      if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
        return;
      }

      // Time to actually perform the action
      moveCard(item.id, page.id);

      // Note: we're mutating the monitor item here!
      // Generally it's better to avoid mutations,
      // but it's good here for the sake of performance
      // to avoid expensive index searches.
      item.index = hoverIndex;
      item.selected = hoverSelected;
    },
  });

  const [{ isDragging }, drag] = useDrag({
    type: 'storyBoardListDnd',
    item: () => {
      return {
        id: page.id,
        index: page.index,
        selected: selected,
      };
    },
    canDrag: () => {
      return canDrag;
    },
    collect: (monitor: any) => ({
      isDragging: monitor.isDragging(),
    }),
    end() {
      moveEnd();
    },
  });
  const opacity = isDragging ? 0.1 : 1;
  // 使用 drag 和 drop 包装 ref
  drag(drop(ItemRef));

  return (
    <ItemWrap selected={selected}>
      <div
        className="item"
        ref={ItemRef}
        data-handler-id={handlerId}
        style={{ opacity }}
      >
        <p>{index}</p>
        <h4>{page.name}</h4>
      </div>
    </ItemWrap>
  );
};
export default ThumbnailItem;

const ItemWrap = styled.div<{ selected: boolean }>`
  .item {
    padding-bottom: ${SPACE_TIMES(8)};
    margin-bottom: ${SPACE_MD};
    cursor: pointer;
    background-color: ${p =>
      p.selected ? p.theme.emphasisBackground : p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadowBlock};

    h4 {
      padding: ${SPACE_XS};
      font-weight: ${FONT_WEIGHT_MEDIUM};
      line-height: ${LINE_HEIGHT_BODY};
      color: ${p => p.theme.textColorSnd};
      text-align: center;
    }

    p {
      padding: ${SPACE_XS};
      font-size: ${FONT_SIZE_TITLE};
      font-weight: ${FONT_WEIGHT_MEDIUM};
      color: ${p => p.theme.textColorDisabled};
    }

    &:hover {
      background-color: ${p => p.theme.emphasisBackground};

      h4 {
        color: ${p => p.theme.textColor};
      }

      p {
        color: ${p => p.theme.textColorLight};
      }
    }
  }
`;
