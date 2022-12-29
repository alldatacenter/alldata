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

import { Space } from 'antd';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import ChartDraggableElementField from './ChartDraggableElementField';

const ChartDraggableElementHierarchy: FC<{
  modalSize;
  config;
  columnConfig;
  ancestors;
  aggregation;
  availableSourceFunctions;
  onConfigChanged;
  handleOpenActionModal;
}> = memo(
  ({
    modalSize,
    config,
    columnConfig,
    ancestors,
    aggregation,
    availableSourceFunctions,
    onConfigChanged,
    handleOpenActionModal,
  }) => {
    const renderChildren = () => {
      return columnConfig?.children?.map(child => {
        const contentProps = {
          modalSize: modalSize,
          config: config,
          columnConfig: child,
          ancestors: ancestors,
          aggregation: aggregation,
          availableSourceFunctions,
          onConfigChanged: onConfigChanged,
          handleOpenActionModal: handleOpenActionModal,
        };
        return <ChartDraggableElementField {...contentProps} />;
      });
    };

    return (
      <div key={columnConfig.uid}>
        <StyledGroupName>{columnConfig.colName}</StyledGroupName>
        <Space direction="vertical" size="small">
          {renderChildren()}
        </Space>
      </div>
    );
  },
);

export default ChartDraggableElementHierarchy;

const StyledGroupName = styled.div`
  text-align: left;
`;
