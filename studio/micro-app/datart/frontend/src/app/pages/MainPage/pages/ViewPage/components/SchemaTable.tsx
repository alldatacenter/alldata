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
  CalendarOutlined,
  FieldStringOutlined,
  NumberOutlined,
} from '@ant-design/icons';
import { TableColumnType, TableProps } from 'antd';
import { ToolbarButton } from 'app/components';
import { VirtualTable } from 'app/components/VirtualTable';
import { DataViewFieldType } from 'app/constants';
import { TABLE_DATA_INDEX } from 'globalConstants';
import { memo, ReactElement, useMemo } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_BASE,
  SPACE,
  SPACE_XS,
  WARNING,
} from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import { Column, ColumnsModel, Model } from '../slice/types';
import { getColumnWidthMap, getHierarchyColumn } from '../utils';
import SetFieldType from './SetFieldType';

const ROW_KEY = 'DATART_ROW_KEY';

interface SchemaTableProps extends TableProps<object> {
  height: number;
  width: number;
  model: ColumnsModel;
  hierarchy: Model;
  dataSource?: object[];
  hasCategory?: boolean;
  hasFormat?: boolean;
  getExtraHeaderActions?: (
    name: string,
    column: Omit<Column, 'name'>,
  ) => ReactElement[];
  onSchemaTypeChange: (
    namePath: string,
    column: Omit<Column, 'name'>,
  ) => (namePath: string[]) => void;
}

export const SchemaTable = memo(
  ({
    height,
    width: propsWidth,
    model,
    hierarchy,
    dataSource,
    hasFormat,
    hasCategory,
    getExtraHeaderActions,
    onSchemaTypeChange,
    ...tableProps
  }: SchemaTableProps) => {
    const dataSourceWithKey = useMemo(
      () =>
        dataSource?.map((o, index) => ({
          ...o,
          [ROW_KEY]: uuidv4(),
          [TABLE_DATA_INDEX]: index + 1,
        })),
      [dataSource],
    );
    const columnWidthMap = useMemo(
      () => getColumnWidthMap(model, dataSource || []),
      [model, dataSource],
    );
    const indexColumnWidth = 50;
    const {
      columns,
      tableWidth,
    }: {
      columns: TableColumnType<object>[];
      tableWidth: number;
    } = useMemo(() => {
      let tableWidth = 0;
      const columns = Object.entries(model).map(([name, column]) => {
        const hierarchyColumn = getHierarchyColumn(name, hierarchy) || column;

        const width = columnWidthMap[name];
        tableWidth += width;

        let icon;
        switch (hierarchyColumn.type) {
          case DataViewFieldType.NUMERIC:
            icon = <NumberOutlined />;
            break;
          case DataViewFieldType.DATE:
            icon = <CalendarOutlined />;
            break;
          default:
            icon = <FieldStringOutlined />;
            break;
        }

        const extraActions =
          getExtraHeaderActions && getExtraHeaderActions(name, hierarchyColumn);

        const title = (
          <>
            <span className="content">{name}</span>
            <SetFieldType
              field={hierarchyColumn as Column}
              hasCategory={hasCategory}
              hasFormat={hasFormat}
              onChange={onSchemaTypeChange(name, hierarchyColumn)}
              icon={
                <ToolbarButton
                  size="small"
                  iconSize={FONT_SIZE_BASE}
                  className="suffix"
                  icon={icon}
                />
              }
            />
            {extraActions}
          </>
        );

        return {
          title,
          dataIndex: name,
          width,
          align:
            column.type === DataViewFieldType.NUMERIC
              ? ('right' as const)
              : ('left' as const),
        };
      });

      return {
        columns: [
          {
            align: 'left',
            dataIndex: TABLE_DATA_INDEX,
            width: indexColumnWidth,
          },
          ...columns,
        ],
        tableWidth,
      };
    }, [
      model,
      hierarchy,
      columnWidthMap,
      hasCategory,
      hasFormat,
      getExtraHeaderActions,
      onSchemaTypeChange,
    ]);

    return (
      <VirtualTable
        {...tableProps}
        rowKey={ROW_KEY}
        size="small"
        components={{ header: { cell: TableHeader } }}
        dataSource={dataSourceWithKey}
        columns={columns}
        scroll={{ x: tableWidth + indexColumnWidth, y: height }}
        width={propsWidth}
      />
    );
  },
);

function TableHeader(props) {
  return (
    <TH className="ant-table-cell">
      <div className="content-wrapper">{props.children}</div>
    </TH>
  );
}

const TH = styled.th`
  padding: ${SPACE_XS} ${SPACE} ${SPACE_XS} ${SPACE_XS} !important;

  .content-wrapper {
    display: flex;
    align-items: center;

    .btn {
      &:hover {
        background-color: ${p => p.theme.emphasisBackground};
      }
    }

    .content {
      flex: 1;
    }

    .prefix {
      margin-right: ${SPACE};
    }

    .suffix {
      margin-left: ${SPACE};
    }

    .partial {
      color: ${WARNING};
    }
  }
`;
