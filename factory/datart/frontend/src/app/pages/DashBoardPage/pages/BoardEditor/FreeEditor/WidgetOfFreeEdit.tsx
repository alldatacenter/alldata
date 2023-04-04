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

import { WidgetMapper } from 'app/pages/DashBoardPage/components/WidgetMapper/WidgetMapper';
import { WidgetContext } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetProvider';
import { WIDGET_DRAG_HANDLE } from 'app/pages/DashBoardPage/constants';
import { fillPx } from 'app/pages/DashBoardPage/utils';
import { getFreeWidgetStyle } from 'app/pages/DashBoardPage/utils/widget';
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { DraggableCore, DraggableEventHandler } from 'react-draggable';
import { useSelector } from 'react-redux';
import { Resizable, ResizeCallbackData } from 'react-resizable';
import styled from 'styled-components/macro';
import { LEVEL_DASHBOARD_EDIT_OVERLAY, WHITE } from 'styles/StyleConstants';
import { WidgetActionContext } from '../../../components/ActionProvider/WidgetActionProvider';
import { BoardScaleContext } from '../../../components/FreeBoardBackground';
import { WidgetInfoContext } from '../../../components/WidgetProvider/WidgetInfoProvider';
import { ORIGINAL_TYPE_MAP } from '../../../constants';
import { widgetMove, widgetMoveEnd } from '../slice/events';
import { selectEditingWidgetIds, selectSelectedIds } from '../slice/selectors';
export enum DragTriggerTypes {
  MouseMove = 'mousemove',
  KeyDown = 'keydown',
}

export const WidgetOfFreeEdit: React.FC<{}> = () => {
  const selectedIds = useSelector(selectSelectedIds);
  const widget = useContext(WidgetContext);
  const { editing: widgetEditing } = useContext(WidgetInfoContext);
  const { onEditFreeWidgetRect } = useContext(WidgetActionContext);
  const editingWidgetIds = useSelector(selectEditingWidgetIds);
  const scale = useContext(BoardScaleContext);
  const hideHandle = useMemo(() => {
    return (
      widgetEditing && widget.config.originalType === ORIGINAL_TYPE_MAP.group
    );
  }, [widget.config.originalType, widgetEditing]);
  const { x, y, width, height } = widget.config.rect;
  const [curXY, setCurXY] = useState<[number, number]>([
    widget.config.rect.x,
    widget.config.rect.y,
  ]);
  const curXYRef = useRef<[number, number]>([0, 0]);
  const [curW, setCurW] = useState(widget.config.rect.width);
  const [curH, setCurH] = useState(widget.config.rect.height);
  useEffect(() => {
    setCurXY([x, y]);
    curXYRef.current = [x, y];
    setCurW(width);
    setCurH(height);
  }, [height, width, x, y]);

  const move = useCallback(
    (selectedIds: string[], deltaX: number, deltaY: number) => {
      if (!selectedIds.includes(widget.id)) return;
      setCurXY(c => [c[0] + deltaX, c[1] + deltaY]);
    },
    [widget.id],
  );
  const moveEnd = useCallback(() => {
    if (!selectedIds.includes(widget.id)) {
      return;
    }
    const nextRect = {
      ...widget.config.rect,
      x: Number(curXY[0].toFixed(1)),
      y: Number(curXY[1].toFixed(1)),
    };
    onEditFreeWidgetRect(nextRect, widget.id, false);
  }, [curXY, onEditFreeWidgetRect, selectedIds, widget.config.rect, widget.id]);
  useEffect(() => {
    widgetMove.on(move);
    widgetMoveEnd.on(moveEnd);
    return () => {
      widgetMove.off(move);
      widgetMoveEnd.off(moveEnd);
    };
  }, [move, moveEnd]);

  const dragStart: DraggableEventHandler = useCallback((e, data) => {
    e.stopPropagation();
    if (e.target === data.node.lastElementChild) {
      return false;
    }
    if (
      typeof (e as MouseEvent).button === 'number' &&
      (e as MouseEvent).button !== 0
    ) {
      return false;
    }
  }, []);
  const drag: DraggableEventHandler = useCallback(
    (e, data) => {
      e.stopPropagation();
      const { deltaX, deltaY } = data;
      widgetMove.emit(selectedIds.concat(widget.id), deltaX, deltaY);
    },
    [selectedIds, widget.id],
  );
  const dragStop: DraggableEventHandler = (e, data) => {
    if (curXYRef.current[0] === curXY[0] && curXYRef.current[1] === curXY[1]) {
      // no change
      return;
    }
    widgetMoveEnd.emit();
    e.stopPropagation();
  };

  const resize = useCallback(
    (e: React.SyntheticEvent, data: ResizeCallbackData) => {
      e.stopPropagation();
      setCurW(c => data.size.width);
      setCurH(c => data.size.height);
    },
    [],
  );
  const resizeStop = useCallback(
    (e: React.SyntheticEvent, { size }: ResizeCallbackData) => {
      e.stopPropagation();
      const nextRect = {
        ...widget.config.rect,
        width: Number(size.width.toFixed(1)),
        height: Number(size.height.toFixed(1)),
      };
      onEditFreeWidgetRect(nextRect, widget.id, false);
    },
    [onEditFreeWidgetRect, widget.config.rect, widget.id],
  );
  const widgetStyle = getFreeWidgetStyle(widget);
  const style: React.CSSProperties = {
    ...widgetStyle,
    left: fillPx(curXY[0]),
    top: fillPx(curXY[1]),
    width: fillPx(curW),
    height: fillPx(curH),
  };
  const lock = widget.config.lock;
  const ssp = e => {
    e.stopPropagation();
  };

  if (editingWidgetIds?.includes(widget?.id)) {
    style['zIndex'] = LEVEL_DASHBOARD_EDIT_OVERLAY + 1;
  }

  return (
    <DraggableCore
      allowAnyClick
      grid={[1, 1]}
      scale={scale[0]}
      onStart={dragStart}
      onDrag={drag}
      onStop={dragStop}
      handle={`.${WIDGET_DRAG_HANDLE}`}
      disabled={lock}
    >
      <Resizable
        axis={'both'}
        width={curW}
        height={curH}
        onResize={resize}
        onResizeStop={resizeStop}
        draggableOpts={{ grid: [1, 1], scale: scale[0], disabled: lock }}
        minConstraints={[50, 50]}
        handleSize={undefined}
        // handleSize={[20, 20]}
        resizeHandles={undefined}
        // resizeHandles={['se']}
        lockAspectRatio={false}
      >
        <ItemWrap style={style} onClick={ssp} hideHandle={hideHandle}>
          <WidgetMapper boardEditing={true} hideTitle={false} />
        </ItemWrap>
      </Resizable>
    </DraggableCore>
  );
};

export default WidgetOfFreeEdit;

const ItemWrap = styled.div<{ hideHandle?: boolean }>`
  box-sizing: border-box;

  & > span:last-child {
    z-index: 999999;
  }

  /* react-resizable style  */

  .react-resizable {
    position: relative;
  }

  & > .react-resizable-handle {
    position: absolute;
    box-sizing: border-box;
    display: ${p => (p.hideHandle ? 'none' : 'block')};
    width: 20px;
    height: 20px;
    padding: 0 3px 3px 0;

    background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2IDYiIHN0eWxlPSJiYWNrZ3JvdW5kLWNvbG9yOiNmZmZmZmYwMCIgeD0iMHB4IiB5PSIwcHgiIHdpZHRoPSI2cHgiIGhlaWdodD0iNnB4Ij48ZyBvcGFjaXR5PSIwLjMwMiI+PHBhdGggZD0iTSA2IDYgTCAwIDYgTCAwIDQuMiBMIDQgNC4yIEwgNC4yIDQuMiBMIDQuMiAwIEwgNiAwIEwgNiA2IEwgNiA2IFoiIGZpbGw9IiMwMDAwMDAiLz48L2c+PC9zdmc+');
    background-repeat: no-repeat;
    background-position: bottom right;
    background-origin: content-box;
  }

  &:hover .react-resizable-handle {
    background-color: ${WHITE};
  }

  .react-resizable-handle-se {
    right: 0;
    bottom: 0;
    cursor: se-resize;
  }
`;
