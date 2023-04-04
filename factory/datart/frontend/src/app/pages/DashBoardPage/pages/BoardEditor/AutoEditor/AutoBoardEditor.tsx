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

import { Empty } from 'antd';
import { useGridWidgetHeight } from 'app/hooks/useGridWidgetHeight';
import { BoardConfigValContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardConfigProvider';
import { BoardInfoContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardInfoProvider';
import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import StyledBackground from 'app/pages/DashBoardPage/components/WidgetComponents/StyledBackground';
import { WidgetWrapProvider } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetWrapProvider';
import {
  LAYOUT_COLS_MAP,
  WIDGET_DRAG_HANDLE,
} from 'app/pages/DashBoardPage/constants';
import useBoardScroll from 'app/pages/DashBoardPage/hooks/useBoardScroll';
import useEditAutoLayoutMap from 'app/pages/DashBoardPage/hooks/useEditAutoLayoutMap';
import useGridLayoutMap from 'app/pages/DashBoardPage/hooks/useGridLayoutMap';
import { DeviceType } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { getBoardMarginPadding } from 'app/pages/DashBoardPage/utils/board';
import { dispatchResize } from 'app/utils/dispatchResize';
import debounce from 'lodash/debounce';
import {
  memo,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from 'react';
import RGL, { Layout, WidthProvider } from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import { useDispatch, useSelector } from 'react-redux';
import 'react-resizable/css/styles.css';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  LEVEL_100,
  LEVEL_DASHBOARD_EDIT_OVERLAY,
  SPACE_MD,
  SPACE_XS,
} from 'styles/StyleConstants';
import { isEmptyArray } from 'utils/object';
import BoardOverlay from '../components/BoardOverlay';
import DeviceList from '../components/DeviceList';
import { editBoardStackActions, editDashBoardInfoActions } from '../slice';
import { selectEditingWidgetIds } from '../slice/selectors';
import { WidgetOfAutoEditor } from './WidgetOfAutoEditor';

const ReactGridLayout = WidthProvider(RGL);

export const AutoBoardEditor: React.FC<{}> = memo(() => {
  const dispatch = useDispatch();
  const { boardId } = useContext(BoardContext);
  const boardConfig = useContext(BoardConfigValContext);
  const { background, allowOverlap } = boardConfig;
  const { deviceType } = useContext(BoardInfoContext);
  const editingWidgetIds = useSelector(selectEditingWidgetIds);
  const { ref, widgetRowHeight, colsKey } = useGridWidgetHeight();

  const { curMargin, curPadding } = useMemo(() => {
    return getBoardMarginPadding(boardConfig, colsKey);
  }, [boardConfig, colsKey]);

  const currentLayout = useRef<Layout[]>([]);

  const { gridWrapRef, thEmitScroll } = useBoardScroll(boardId);

  const [curWH, setCurWH] = useState<number[]>([]);

  const updateCurWH = useCallback((values: number[]) => {
    setCurWH(values);
    setImmediate(() => {
      dispatchResize();
    });
  }, []);

  const sortedLayoutWidgets = useEditAutoLayoutMap(boardId);
  const layoutMap = useGridLayoutMap(sortedLayoutWidgets);

  const changeWidgetLayouts = debounce((layouts: Layout[]) => {
    dispatch(
      editBoardStackActions.changeAutoBoardWidgetsRect({
        layouts,
        deviceType: deviceType,
      }),
    );
  }, 300);

  const onLayoutChange = (layouts: Layout[]) => {
    currentLayout.current = layouts;
    thEmitScroll();
    // ignore isDraggable item from out
    if (layouts.find(item => item.isDraggable === true)) {
      return;
    }
    dispatch(editDashBoardInfoActions.adjustDashLayouts(layouts));
  };

  const { deviceClassName } = useMemo(() => {
    let deviceClassName: string = 'desktop';
    if (deviceType === DeviceType.Mobile) {
      deviceClassName = 'mobile';
    }
    return {
      deviceClassName,
    };
  }, [deviceType]);

  const boardChildren = useMemo(() => {
    return sortedLayoutWidgets.map(item => {
      // TODO(Stephen): 将外层div与内层WidgetWrapProvider合并，同时修改FreeBoardEditor
      return (
        <div
          style={{
            zIndex: editingWidgetIds?.includes(item?.id)
              ? LEVEL_DASHBOARD_EDIT_OVERLAY + 1
              : 'auto',
          }}
          key={item.id}
        >
          <WidgetWrapProvider
            id={item.id}
            boardEditing={true}
            boardId={boardId}
          >
            <WidgetOfAutoEditor />
          </WidgetWrapProvider>
        </div>
      );
    });
  }, [boardId, editingWidgetIds, sortedLayoutWidgets]);

  /**
   * https://www.npmjs.com/package/react-grid-layout
   */
  return (
    <Wrapper className={deviceClassName}>
      {deviceType === DeviceType.Mobile && (
        <DeviceList updateCurWH={updateCurWH} />
      )}
      <StyledContainer
        bg={background}
        curWH={curWH}
        className={deviceClassName}
        ref={ref}
      >
        {sortedLayoutWidgets.length ? (
          <div style={{ position: 'relative' }}>
            <div className="grid-wrap" ref={gridWrapRef}>
              <ReactGridLayout
                layout={layoutMap[colsKey]}
                cols={LAYOUT_COLS_MAP[colsKey]}
                margin={curMargin}
                containerPadding={curPadding}
                rowHeight={widgetRowHeight}
                useCSSTransforms={true}
                measureBeforeMount={false}
                onDragStop={changeWidgetLayouts}
                onResizeStop={changeWidgetLayouts}
                isBounded={false}
                onLayoutChange={onLayoutChange}
                isDraggable={true}
                isResizable={true}
                allowOverlap={allowOverlap}
                draggableHandle={`.${WIDGET_DRAG_HANDLE}`}
              >
                {boardChildren}
              </ReactGridLayout>
            </div>
            {!isEmptyArray(editingWidgetIds) && <BoardOverlay />}
          </div>
        ) : (
          <div className="empty">
            <Empty description="" />
          </div>
        )}
      </StyledContainer>
    </Wrapper>
  );
});

const Wrapper = styled.div<{}>`
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  width: 100px;
  min-height: 0;

  .react-resizable-handle {
    z-index: ${LEVEL_100};
  }

  &.desktop {
    min-width: 769px;
  }
`;

const StyledContainer = styled(StyledBackground)<{ curWH: number[] }>`
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;

  &.desktop {
    flex: 1;
    width: 100%;
  }

  &.mobile {
    width: ${p => `${p.curWH[0]}px`};
    height: ${p => `${p.curWH[1]}px`};
    margin-top: ${SPACE_MD};
    border: ${SPACE_XS} solid ${p => p.theme.borderColorEmphasis};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadowBlock};
  }

  .grid-wrap {
    flex: 1;
    overflow-y: auto;
    -ms-overflow-style: none;
  }

  .grid-wrap::-webkit-scrollbar {
    width: 0 !important;
  }

  .empty {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
  }
`;
