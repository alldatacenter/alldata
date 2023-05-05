// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React, { useContext } from 'react';
import { DndContext } from 'react-dnd';
import { throttle } from 'lodash';
import { useMount } from 'react-use';

function noop(...args: number[]) {}
function intBetween(min: number, max: number, val: number) {
  return Math.floor(Math.min(max, Math.max(min, val)));
}

function isTouchEvent(evt: MouseEvent | TouchEvent): evt is TouchEvent {
  return evt.type === 'touchmove';
}

function getCoords(evt: MouseEvent | TouchEvent) {
  if (isTouchEvent(evt)) {
    return { x: evt.changedTouches[0].clientX, y: evt.changedTouches[0].clientY };
  }
  return { x: evt.clientX, y: evt.clientY };
}

// this easing function is from https://gist.github.com/gre/1650294 and
// expects/returns a number between [0, 1], however strength functions
// expects/returns a value between [-1, 1]
function easeFn(val: number) {
  const t = (val + 1) / 2; // [-1, 1] -> [0, 1]
  const easedT = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
  return easedT * 2 - 1; // [0, 1] -> [-1, 1]
}

interface Box {
  x: number;
  w: number;
  y: number;
  h: number;
}
function createHorizontalStrength(_buffer: number) {
  return function genHorizontalStrength({ x, w, y, h }: Box, point: { x: number; y: number }) {
    const buffer = Math.min(w / 2, _buffer);
    const inRange = point.x >= x && point.x <= x + w;
    const inBox = inRange && point.y >= y && point.y <= y + h;
    if (inBox) {
      if (point.x < x + buffer) {
        return (point.x - x - buffer) / buffer;
      }
      if (point.x > x + w - buffer) {
        return -(x + w - point.x - buffer) / buffer;
      }
    }
    return 0;
  };
}

function createVerticalStrength(_buffer: number) {
  return function genVerticalStrength({ y, h, x, w }: Box, point: { x: number; y: number }) {
    const buffer = Math.min(h / 2, _buffer);
    const inRange = point.y >= y && point.y <= y + h;
    const inBox = inRange && point.x >= x && point.x <= x + w;
    if (inBox) {
      if (point.y < y + buffer) {
        return (point.y - y - buffer) / buffer;
      }
      if (point.y > y + h - buffer) {
        return -(y + h - point.y - buffer) / buffer;
      }
    }
    return 0;
  };
}
const DEFAULT_BUFFER = 240;

export default function createScrollingComponent(WrappedComponent: string) {
  const ScrollingComponent = ({
    horizontalBuffer = DEFAULT_BUFFER,
    verticalBuffer = DEFAULT_BUFFER,
    strengthMultiplier = 60,
    onScrollChange = noop,
    ease = true,
    ...rest
  }) => {
    const { dragDropManager } = useContext(DndContext);
    const staticRef = React.useRef({
      scaleX: 0,
      scaleY: 0,
      frame: 0,
      attached: false,
      dragging: false,
      container: null,
    });

    useMount(() => {
      const horizontalStrength = createHorizontalStrength(horizontalBuffer);
      const verticalStrength = createVerticalStrength(verticalBuffer);
      const handleEvent = (evt: MouseEvent | TouchEvent) => {
        if (staticRef.current.dragging && !staticRef.current.attached) {
          attach();
          updateScrolling(evt);
        }
      };

      const attach = () => {
        staticRef.current.attached = true;
        window.document.body.addEventListener('dragover', updateScrolling);
        window.document.body.addEventListener('touchmove', updateScrolling);
      };

      const detach = () => {
        staticRef.current.attached = false;
        window.document.body.removeEventListener('dragover', updateScrolling);
        window.document.body.removeEventListener('touchmove', updateScrolling);
      };

      const container = staticRef.current.container as unknown as HTMLDivElement;
      // Update scaleX and scaleY every 100ms or so
      // and start scrolling if necessary
      const updateScrolling = throttle(
        (evt) => {
          const { left: x, top: y, width: w, height: h } = container.getBoundingClientRect();
          const box = { x, y, w, h };
          const coords = getCoords(evt);
          // calculate strength
          staticRef.current.scaleX = horizontalStrength(box, coords);
          staticRef.current.scaleY = verticalStrength(box, coords);
          if (ease) {
            staticRef.current.scaleX = easeFn(staticRef.current.scaleX);
            staticRef.current.scaleY = easeFn(staticRef.current.scaleY);
          }
          // start scrolling if we need to
          if (!staticRef.current.frame && (staticRef.current.scaleX || staticRef.current.scaleY)) {
            startScrolling();
          }
        },
        100,
        {
          trailing: false,
        },
      );

      const startScrolling = () => {
        let i = 0;
        const tick = () => {
          const { scaleX, scaleY } = staticRef.current;
          // stop scrolling if there's nothing to do
          if (strengthMultiplier === 0 || scaleX + scaleY === 0) {
            stopScrolling();
            return;
          }
          // there's a bug in safari where it seems like we can't get
          // mousemove events from a container that also emits a scroll
          // event that same frame. So we double the strengthMultiplier and only adjust
          // the scroll position at 30fps
          if (i++ % 2) {
            const { scrollLeft, scrollTop, scrollWidth, scrollHeight, clientWidth, clientHeight } = container;
            const newLeft = scaleX
              ? (container.scrollLeft = intBetween(
                  0,
                  scrollWidth - clientWidth,
                  scrollLeft + scaleX * strengthMultiplier,
                ))
              : scrollLeft;
            const newTop = scaleY
              ? (container.scrollTop = intBetween(
                  0,
                  scrollHeight - clientHeight,
                  scrollTop + scaleY * strengthMultiplier,
                ))
              : scrollTop;
            onScrollChange(newLeft, newTop);
          }
          staticRef.current.frame = window.requestAnimationFrame(tick);
        };
        tick();
      };

      const stopScrolling = () => {
        detach();
        staticRef.current.scaleX = 0;
        staticRef.current.scaleY = 0;
        if (staticRef.current.frame) {
          window.cancelAnimationFrame(staticRef.current.frame);
          staticRef.current.frame = 0;
        }
      };

      container.addEventListener('dragover', handleEvent);
      // touchmove events don't seem to work across siblings, so we unfortunately
      // have to attach the listeners to the body
      window.document.body.addEventListener('touchmove', handleEvent);
      const clearMonitorSubscription = dragDropManager.getMonitor().subscribeToStateChange(() => {
        const isDragging = dragDropManager.getMonitor().isDragging();
        if (!staticRef.current.dragging && isDragging) {
          staticRef.current.dragging = true;
        } else if (staticRef.current.dragging && !isDragging) {
          staticRef.current.dragging = false;
          stopScrolling();
        }
      });
      return () => {
        container.removeEventListener('dragover', handleEvent);
        window.document.body.removeEventListener('touchmove', handleEvent);
        clearMonitorSubscription();
        stopScrolling();
      };
    });

    return (
      <WrappedComponent
        ref={(ref: any) => {
          staticRef.current.container = ref;
        }}
        {...rest}
      />
    );
  };
  ScrollingComponent.displayName = `Scrolling${WrappedComponent}`;
  return ScrollingComponent;
}
