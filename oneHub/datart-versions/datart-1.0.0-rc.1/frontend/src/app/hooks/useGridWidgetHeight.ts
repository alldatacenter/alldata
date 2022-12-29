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

import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import {
  BASE_ROW_HEIGHT,
  BASE_VIEW_WIDTH,
  BREAK_POINT_MAP,
  LAYOUT_COLS_KEYS,
  MIN_ROW_HEIGHT,
} from 'app/pages/DashBoardPage/constants';
import { boardActions } from 'app/pages/DashBoardPage/pages/Board/slice';
import { ColsKeyType } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { useContext, useLayoutEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import useResizeObserver from './useResizeObserver';

export const useGridWidgetHeight = () => {
  const { boardId, renderMode } = useContext(BoardContext);
  const dispatch = useDispatch();
  const [colsKey, setColsKey] = useState<ColsKeyType>(LAYOUT_COLS_KEYS[0]);
  const [cacheW, setCacheW] = useState(0);
  const { ref, width = 0, height = 0 } = useResizeObserver<HTMLDivElement>();

  useLayoutEffect(() => {
    if (width > 0 && height > 0) {
      setCacheW(width);
      //  BREAK_POINT_MAP['sm']=768px
      if (width <= BREAK_POINT_MAP['sm']) {
        setColsKey('sm');
      } else {
        setColsKey('lg');
      }
      if (renderMode !== 'edit') {
        dispatch(
          boardActions.setBoardWidthHeight({ boardId, wh: [width, height] }),
        );
      }
    }
  }, [width, height, dispatch, boardId, renderMode]);

  const widgetRowHeight = useMemo(() => {
    let dynamicHeight = (cacheW * BASE_ROW_HEIGHT) / BASE_VIEW_WIDTH;
    return Math.max(dynamicHeight, MIN_ROW_HEIGHT);
  }, [cacheW]);

  return {
    ref,
    widgetRowHeight,
    colsKey,
  };
};
