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
import useResizeObserver from 'app/hooks/useResizeObserver';
import { useContext, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { BoardContext } from '../components/BoardProvider/BoardProvider';
import { boardActions } from '../pages/Board/slice';

export default function useBoardWidthHeight() {
  const { boardId, renderMode } = useContext(BoardContext);

  const dispatch = useDispatch();

  const {
    ref: gridRef,
    width: gridWidth = 800,
    height: gridHeight = 200,
  } = useResizeObserver<HTMLDivElement>({
    refreshMode: 'debounce',
    refreshRate: 100,
  });

  useEffect(() => {
    const width = gridWidth;
    const height = gridHeight;
    // TODO in only in  renderMode
    if (renderMode !== 'edit') {
      dispatch(
        boardActions.setBoardWidthHeight({ boardId, wh: [width, height] }),
      );
    }
  }, [gridHeight, dispatch, boardId, gridWidth, renderMode]);
  return { gridRef };
}
