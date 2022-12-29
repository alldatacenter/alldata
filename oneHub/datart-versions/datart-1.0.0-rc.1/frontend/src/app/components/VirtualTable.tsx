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

import { Empty, Table, TableProps } from 'antd';
import classNames from 'classnames';
import { TABLE_DATA_INDEX } from 'globalConstants';
import React, {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { VariableSizeGrid as Grid } from 'react-window';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';

interface VirtualTableProps extends TableProps<object> {
  width: number;
  scroll: { x: number; y: number };
  columns: any;
}

/**
 * Table组件中使用了虚拟滚动条 渲染的速度变快 基于（react-windows）
 * 使用方法：import { VirtualTable } from 'app/components/VirtualTable';
 * <VirtualTable
    dataSource={dataSourceWithKey}
    columns={columns}
    width={width}
    ...tableProps
  />
 */
export const VirtualTable = memo((props: VirtualTableProps) => {
  const { columns, scroll, width: boxWidth, dataSource } = props;
  const widthColumns = columns.map(v => {
    return { width: v.width, dataIndex: v.dataIndex };
  });
  const gridRef: any = useRef();
  const isFull = useRef<boolean>(false);
  const widthColumnCount = widthColumns.filter(
    ({ width, dataIndex }) => !width || dataIndex !== TABLE_DATA_INDEX,
  ).length;
  const [connectObject] = useState(() => {
    const obj = {};
    Object.defineProperty(obj, 'scrollLeft', {
      get: () => null,
      set: scrollLeft => {
        if (gridRef.current) {
          gridRef.current.scrollTo({
            scrollLeft,
          });
        }
      },
    });
    return obj;
  });
  isFull.current = boxWidth > scroll.x;

  if (isFull.current === true) {
    widthColumns.forEach((v, i) => {
      return (widthColumns[i].width =
        widthColumns[i].dataIndex === TABLE_DATA_INDEX
          ? widthColumns[i].width
          : widthColumns[i].width + (boxWidth - scroll.x) / widthColumnCount);
    });
  }

  const mergedColumns = useMemo(() => {
    return columns.map((column, i) => {
      return {
        ...column,
        width: column.width
          ? widthColumns[i].width
          : Math.floor(boxWidth / widthColumnCount),
      };
    });
  }, [boxWidth, columns, widthColumnCount, widthColumns]);

  const resetVirtualGrid = useCallback(() => {
    gridRef.current?.resetAfterIndices({
      columnIndex: 0,
      shouldForceUpdate: true,
    });
  }, [gridRef]);

  useEffect(() => resetVirtualGrid, [boxWidth, dataSource, resetVirtualGrid]);

  const renderVirtualList = useCallback(
    (rawData, { scrollbarSize, ref, onScroll }) => {
      ref.current = connectObject;
      const totalHeight = rawData.length * 39;

      if (!dataSource?.length) {
        //If the data is empty
        return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
      }

      return (
        <Grid
          ref={gridRef}
          className="virtual-grid"
          columnCount={mergedColumns.length}
          columnWidth={index => {
            const { width } = mergedColumns[index];
            return totalHeight > scroll.y && index === mergedColumns.length - 1
              ? width - scrollbarSize - 16
              : width;
          }}
          height={scroll.y}
          rowCount={rawData.length}
          rowHeight={() => 39}
          width={boxWidth}
          onScroll={({ scrollLeft }) => {
            onScroll({
              scrollLeft,
            });
          }}
        >
          {({ rowIndex, columnIndex, style }) => {
            style = {
              padding: `${SPACE_TIMES(2)}`,
              textAlign: mergedColumns[columnIndex].align,
              ...style,
            };
            return (
              <TableCell
                className={classNames('virtual-table-cell', {
                  'virtual-table-cell-last':
                    columnIndex === mergedColumns.length - 1,
                })}
                style={style}
                key={columnIndex}
              >
                {rawData[rowIndex][mergedColumns[columnIndex].dataIndex]}
              </TableCell>
            );
          }}
        </Grid>
      );
    },
    [mergedColumns, boxWidth, connectObject, dataSource, scroll],
  );

  return (
    <Table
      {...props}
      columns={mergedColumns}
      components={{
        body: renderVirtualList,
        ...props.components,
      }}
    />
  );
});

const TableCell = styled.div`
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};
`;
