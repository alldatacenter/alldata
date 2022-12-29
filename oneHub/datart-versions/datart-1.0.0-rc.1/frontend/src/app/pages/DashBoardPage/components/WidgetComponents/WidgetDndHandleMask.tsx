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
import { CONTAINER_TAB } from 'app/pages/DashBoardPage/constants';
import React, { memo } from 'react';
import { useDrag } from 'react-dnd';
import { LEVEL_10 } from 'styles/StyleConstants';
export interface WidgetDndHandleMaskProps {
  widgetId: string;
  canWrapped: boolean;
}
export interface DropItem {
  childId: string;
  canWrapped: boolean;
}
export const WidgetDndHandleMask: React.FC<WidgetDndHandleMaskProps> = memo(
  ({ widgetId, canWrapped }) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const [_, dragRef, dragPreview] = useDrag(() => ({
      type: CONTAINER_TAB,
      item: { canWrapped: canWrapped, childId: widgetId } as DropItem,
      end: (opt, monitor) => {},
      collect: monitor => ({
        isDragging: monitor.isDragging(),
        handlerId: monitor.getHandlerId(),
      }),
    }));

    const ssp = e => {
      e.stopPropagation();
    };
    return (
      <>
        <div
          ref={dragPreview}
          className="dragRef2"
          onClick={ssp}
          onDragStart={ssp}
          style={{
            position: 'absolute',
            top: '0',
            cursor: 'grabbing',
            width: '100%',
            height: '100%',
          }}
        ></div>
        <div
          ref={dragRef}
          // onDragStart={ssp}
          onClick={ssp}
          className="dragRef1"
          style={{
            position: 'absolute',
            cursor: 'grabbing',
            top: '0',
            zIndex: LEVEL_10 + 1,
            width: '100%',
            height: '100%',
          }}
        ></div>
      </>
    );
  },
);
