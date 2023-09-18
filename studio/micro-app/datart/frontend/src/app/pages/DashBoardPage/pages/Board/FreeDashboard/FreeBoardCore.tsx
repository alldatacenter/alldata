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
import { BoardConfigValContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardConfigProvider';
import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import { WidgetWrapProvider } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetWrapProvider';
import useBoardWidthHeight from 'app/pages/DashBoardPage/hooks/useBoardWidthHeight';
import useLayoutMap from 'app/pages/DashBoardPage/hooks/useLayoutMap';
import { memo, useContext, useMemo } from 'react';
import styled from 'styled-components/macro';
import SlideBackground from '../../../components/FreeBoardBackground';
import useClientRect from '../../../hooks/useClientRect';
import useSlideStyle from '../../../hooks/useSlideStyle';
import { WidgetOfFree } from './WidgetOfFree';
import ZoomControl from './ZoomControl';

export interface FreeBoardCoreProps {
  boardId: string;
  showZoomCtrl?: boolean;
}
export const FreeBoardCore: React.FC<FreeBoardCoreProps> = memo(
  ({ boardId, showZoomCtrl }) => {
    const {
      width: slideWidth,
      height: slideHeight,
      scaleMode,
    } = useContext(BoardConfigValContext);
    const { editing, autoFit } = useContext(BoardContext);

    const layoutWidgets = useLayoutMap(boardId);

    const [rect, refGridBackground] = useClientRect<HTMLDivElement>();
    const {
      zoomIn,
      zoomOut,
      sliderChange,
      sliderValue,
      scale,
      nextBackgroundStyle,
      slideTranslate,
    } = useSlideStyle(
      autoFit,
      editing,
      rect,
      slideWidth,
      slideHeight,
      scaleMode,
    );
    const boardChildren = useMemo(() => {
      return layoutWidgets.map(item => {
        return (
          <WidgetWrapProvider
            key={item.id}
            id={item.id}
            boardEditing={editing}
            boardId={boardId}
          >
            <WidgetOfFree />
          </WidgetWrapProvider>
        );
      });
    }, [layoutWidgets, editing, boardId]);
    const { gridRef } = useBoardWidthHeight();

    return (
      <Wrapper>
        <div className="container" ref={gridRef}>
          <div
            className="grid-background"
            style={nextBackgroundStyle}
            ref={refGridBackground}
          >
            <SlideBackground scale={scale} slideTranslate={slideTranslate}>
              {layoutWidgets.length ? (
                boardChildren
              ) : (
                <div className="empty">
                  <Empty description="" />
                </div>
              )}
            </SlideBackground>
          </div>
          {showZoomCtrl && (
            <ZoomControl
              sliderValue={sliderValue}
              scale={scale}
              zoomIn={zoomIn}
              zoomOut={zoomOut}
              sliderChange={sliderChange}
            />
          )}
        </div>
      </Wrapper>
    );
  },
);

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  overflow-y: hidden;
  .container {
    display: flex;
    flex: 1;
    flex-direction: column;
    overflow-y: hidden;
    .grid-background {
      flex: 1;
      -ms-overflow-style: none;
      overflow-y: hidden;

      .empty {
        display: flex;
        flex: 1;
        align-items: center;
        justify-content: center;
        height: 100%;
      }
    }
    .grid-background::-webkit-scrollbar {
      width: 0 !important;
    }
  }
  .container::-webkit-scrollbar {
    width: 0 !important;
  }
  &::-webkit-scrollbar {
    width: 0 !important;
  }
`;
