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

// Observer DOMRect of element
import debounce from 'lodash/debounce';
import { RefObject, useLayoutEffect, useRef, useState } from 'react';

export const CLIENT_RECT_DEBOUNCE_INTERVAL = 200;

function useClientRect<T extends HTMLElement>(
  debounceInterval: number = CLIENT_RECT_DEBOUNCE_INTERVAL,
  realtime: boolean = false,
): [DOMRect, RefObject<T>] {
  const ref = useRef<T>(null);
  const [rect, setRect] = useState<DOMRect>(new DOMRect());

  useLayoutEffect(() => {
    const resize = debounce(() => {
      if (ref.current) {
        const DomRect = ref.current.getBoundingClientRect();
        if (DomRect.width !== 0) {
          setRect(ref.current.getBoundingClientRect());
        }
      }
    }, debounceInterval);
    if (!ref.current) {
      return;
    }
    resize();
    if (typeof ResizeObserver === 'function') {
      let resizeObserver = new ResizeObserver(resize);
      resizeObserver.observe(ref.current);
      return () => {
        resizeObserver.disconnect();
        //@ts-ignore
        resizeObserver = null;
      };
    } else {
      window.addEventListener('resize', resize);
      return () => {
        window.removeEventListener('resize', resize);
      };
    }
  }, [debounceInterval]);

  return [rect, ref];
}

export default useClientRect;

// refs: https://github.com/rehooks/component-size/blob/master/index.js
