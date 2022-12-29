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
import throttle from 'lodash/throttle';
import { RefObject, useCallback, useEffect, useMemo, useRef } from 'react';
import { boardScroll } from '../pages/BoardEditor/slice/events';
export default function useBoardScroll(boardId: string) {
  const gridWrapRef: RefObject<HTMLDivElement> = useRef(null);

  const onEmitScroll = useCallback(() => {
    if (boardId) {
      boardScroll.emit(boardId);
    }
  }, [boardId]);

  const thEmitScroll = useMemo(
    () => throttle(onEmitScroll, 100),
    [onEmitScroll],
  );

  useEffect(() => {
    const shadowGridWrapRef = gridWrapRef?.current;
    setImmediate(() => {
      shadowGridWrapRef?.removeEventListener('scroll', thEmitScroll, false);
      if (shadowGridWrapRef) {
        shadowGridWrapRef.addEventListener('scroll', thEmitScroll, false);
      }
    });
    return () => {
      if (shadowGridWrapRef) {
        shadowGridWrapRef.removeEventListener('scroll', thEmitScroll, false);
      }
    };
  }, [thEmitScroll]);

  return {
    gridWrapRef,
    thEmitScroll,
  };
}
