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

import {
  BranchesOutlined,
  CalendarOutlined,
  DeleteOutlined,
  FieldStringOutlined,
  FileUnknownOutlined,
  NumberOutlined,
  SisternodeOutlined,
} from '@ant-design/icons';
import { Button, Tooltip } from 'antd';
import { IW } from 'app/components';
import { DataViewFieldType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, memo, useState } from 'react';
import { Draggable } from 'react-beautiful-dnd';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_BASE,
  FONT_SIZE_TITLE,
  INFO,
  SPACE,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
  SUCCESS,
  WARNING,
} from 'styles/StyleConstants';
import SetFieldType from '../../../components/SetFieldType';
import { Column, ColumnRole } from '../../../slice/types';
import { ALLOW_COMBINE_COLUMN_TYPES } from './constant';
const DataModelNode: FC<{
  node: Column;
  className?: string;
  branchRole?: ColumnRole;
  onNodeTypeChange: (type: string[], name: string) => void;
  onMoveToHierarchy: (node: Column) => void;
  onCreateHierarchy?: (node: Column) => void;
  onDeleteFromHierarchy?: (node: Column) => void;
}> = memo(
  ({
    node,
    className,
    branchRole,
    onCreateHierarchy,
    onMoveToHierarchy,
    onNodeTypeChange,
    onDeleteFromHierarchy,
  }) => {
    const t = useI18NPrefix('view.model');
    const [isHover, setIsHover] = useState(false);
    const hasCategory = true;

    const renderNode = (node, isDragging) => {
      let icon;
      switch (node.type) {
        case DataViewFieldType.NUMERIC:
          icon = (
            <NumberOutlined style={{ alignSelf: 'center', color: SUCCESS }} />
          );
          break;
        case DataViewFieldType.STRING:
          icon = (
            <FieldStringOutlined style={{ alignSelf: 'center', color: INFO }} />
          );
          break;
        case DataViewFieldType.DATE:
          icon = (
            <CalendarOutlined style={{ alignSelf: 'center', color: INFO }} />
          );
          break;
        default:
          icon = (
            <FileUnknownOutlined
              style={{ alignSelf: 'center', color: WARNING }}
            />
          );
          break;
      }

      const isAllowCreateHierarchy = node => {
        return ALLOW_COMBINE_COLUMN_TYPES.includes(node.type);
      };

      return (
        <div
          className="content"
          onMouseEnter={() => {
            setIsHover(true);
          }}
          onMouseLeave={() => {
            setIsHover(false);
          }}
        >
          <SetFieldType
            field={node}
            onChange={onNodeTypeChange}
            hasCategory={hasCategory}
            icon={<StyledIW fontSize={FONT_SIZE_TITLE}>{icon}</StyledIW>}
          />
          <span>
            {branchRole === ColumnRole.Hierarchy ? node.name : node.displayName}
          </span>
          <div className="action">
            {isHover &&
              !isDragging &&
              isAllowCreateHierarchy(node) &&
              onCreateHierarchy && (
                <Tooltip title={t('newHierarchy')}>
                  <Button
                    type="link"
                    onClick={() => onCreateHierarchy(node)}
                    icon={<BranchesOutlined />}
                  />
                </Tooltip>
              )}
            {isHover && !isDragging && isAllowCreateHierarchy(node) && (
              <Tooltip title={t('addToHierarchy')}>
                <Button
                  type="link"
                  onClick={() => onMoveToHierarchy(node)}
                  icon={<SisternodeOutlined />}
                />
              </Tooltip>
            )}
            {isHover && !isDragging && onDeleteFromHierarchy && (
              <Tooltip title={t('delete')}>
                <Button
                  type="link"
                  onClick={() => onDeleteFromHierarchy(node)}
                  icon={<DeleteOutlined />}
                />
              </Tooltip>
            )}
          </div>
        </div>
      );
    };

    return (
      <Draggable key={node?.name} draggableId={node?.name} index={node?.index}>
        {(draggableProvided, draggableSnapshot) => (
          <StyledDataModelNode
            isDragging={draggableSnapshot.isDragging}
            className={className}
            style={draggableProvided.draggableProps.style}
            ref={draggableProvided.innerRef}
            {...draggableProvided.draggableProps}
            {...draggableProvided.dragHandleProps}
          >
            {renderNode(node, draggableSnapshot.isDragging)}
          </StyledDataModelNode>
        )}
      </Draggable>
    );
  },
);

export default DataModelNode;

const StyledDataModelNode = styled.div<{
  isDragging: boolean;
}>`
  margin: ${SPACE} ${SPACE_MD};
  font-size: ${FONT_SIZE_BASE};
  line-height: 32px;
  user-select: 'none';
  background: ${p =>
    p.isDragging ? p.theme.highlightBackground : 'transparent'};

  &.in-hierarchy {
    margin-right: 0;
  }

  & .content {
    display: flex;
    align-items: center;
  }

  & .action {
    display: flex;
    flex: 1;
    justify-content: flex-end;
    padding-right: ${SPACE_XS};
  }
`;

const StyledIW = styled(IW)`
  width: ${SPACE_TIMES(7)};
  height: ${SPACE_TIMES(7)};
  margin-right: ${SPACE_XS};
  cursor: pointer;
  border: 1px solid ${p => p.theme.borderColorSplit};
  border-radius: 4px;
`;
