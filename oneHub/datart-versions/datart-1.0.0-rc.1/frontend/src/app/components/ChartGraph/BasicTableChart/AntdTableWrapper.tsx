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

import { Table } from 'antd';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';

interface TableStyleConfigProps {
  odd?: {
    backgroundColor: string;
    color: string;
  };
  even?: {
    backgroundColor: string;
    color: string;
  };
  isFixedColumns?: boolean;
  summaryStyle?: {
    backgroundColor?: string;
    fontFamily?: string;
    fontSize?: string;
    fontWeight?: string;
    fontStyle?: string;
    color?: string;
  };
}

const AntdTableWrapper: FC<{
  dataSource: [];
  columns: [];
  tableStyleConfig?: TableStyleConfigProps | undefined;
  summaryFn?: (data) => { total: number; summarys: [] };
}> = memo(
  ({ dataSource, columns, children, summaryFn, tableStyleConfig, ...rest }) => {
    const getTableSummaryRow = pageData => {
      if (!summaryFn) {
        return undefined;
      }
      const summaryData = summaryFn?.(pageData);
      return (
        <Table.Summary fixed>
          <Table.Summary.Row>
            {(summaryData?.summarys || []).map((data, index) => {
              return (
                <Table.Summary.Cell key={index} index={index}>
                  {data}
                </Table.Summary.Cell>
              );
            })}
          </Table.Summary.Row>
        </Table.Summary>
      );
    };

    return (
      <StyledTable
        {...rest}
        tableStyleConfig={tableStyleConfig}
        dataSource={dataSource}
        columns={columns}
        summary={getTableSummaryRow}
      />
    );
  },
);

const StyledTable = styled(Table)<{ tableStyleConfig?: TableStyleConfigProps }>`
  height: 100%;
  overflow: auto;

  .ant-table {
    background: transparent;
  }
  .ant-table-body {
    overflow: ${p =>
      p?.tableStyleConfig?.isFixedColumns ? 'auto scroll' : 'auto !important'};
  }

  .ant-table .ant-table-container .ant-table-body .ant-table-tbody td {
    background: inherit;
  }

  .ant-table-summary .ant-table-cell {
    font-family: ${p => p?.tableStyleConfig?.summaryStyle?.fontFamily};
    font-size: ${p => p?.tableStyleConfig?.summaryStyle?.fontSize + 'px'};
    font-style: ${p => p?.tableStyleConfig?.summaryStyle?.fontStyle};
    font-weight: ${p => p?.tableStyleConfig?.summaryStyle?.fontWeight};
    color: ${p => p?.tableStyleConfig?.summaryStyle?.color};
    background-color: ${p =>
      p?.tableStyleConfig?.summaryStyle?.backgroundColor};
  }

  .ant-table .ant-table-container .ant-table-body .datart-basic-table-odd {
    color: ${p => p?.tableStyleConfig?.odd?.color || 'inherit'};
    background: ${p =>
      p?.tableStyleConfig?.odd?.backgroundColor || 'transparent'};
  }

  .ant-table .ant-table-container .ant-table-body .datart-basic-table-even {
    color: ${p => p?.tableStyleConfig?.even?.color || 'inherit'};
    background: ${p =>
      p?.tableStyleConfig?.even?.backgroundColor || 'transparent'};
  }
`;

export default AntdTableWrapper;
