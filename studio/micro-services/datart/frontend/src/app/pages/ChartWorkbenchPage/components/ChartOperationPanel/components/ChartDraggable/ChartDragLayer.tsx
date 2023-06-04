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

import { DragLayer } from 'react-dnd';
import styled from 'styled-components';
import { LEVEL_100 } from 'styles/StyleConstants';
import ChartDragPreview from './ChartDragPreview';

const collect = monitor => {
  return {
    item: monitor.getItem(),
    itemType: monitor.getItemType(),
    currentOffset: monitor.getSourceClientOffset(),
    isDragging: monitor.isDragging(),
  };
};

const getItemStyles = currentOffset => {
  if (!currentOffset) {
    return {
      display: 'none',
    };
  }
  const { x, y } = currentOffset;
  return {
    transform: `translate(${x}px, ${y}px)`,
  };
};

function CardDragLayer(props) {
  const { item, itemType, currentOffset, isDragging } = props;

  /**
   * zh: 如果不是正在拖动或者拖动的数据项不是一个数组则不执行
   * en: If it is not being dragged or the data item being dragged is not an array, do not execute
   */
  if (!isDragging || !Array.isArray(item)) {
    return null;
  }

  const renderItem = (type, item) => {
    switch (type) {
      case 'dataset_column':
        return (
          <div style={getItemStyles(currentOffset)}>
            <ChartDragPreview dataItem={item} />
          </div>
        );
      default:
        return null;
    }
  };

  return <LayerStyles>{renderItem(itemType, item)}</LayerStyles>;
}
export default DragLayer(collect)(CardDragLayer);

const LayerStyles = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_100};
  pointer-events: none;
`;
