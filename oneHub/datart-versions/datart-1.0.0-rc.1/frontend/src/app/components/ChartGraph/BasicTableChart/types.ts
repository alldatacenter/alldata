import { AggregateFieldActionType } from 'app/constants';
import { ChartDataSectionField, FontStyle } from 'app/types/ChartConfig';

export interface TableStyle {
  odd: { backgroundColor: string; color: string };
  even: { backgroundColor: string; color: string };
  isFixedColumns: boolean;
  summaryStyle: { backgroundColor: string } & FontStyle;
}

export interface TableColumnsList {
  uid?: string | undefined;
  children?: Array<TableColumnsList>;
  isGroup?: boolean;
  sorter?: boolean;
  showSorterTooltip?: boolean;
  title?: JSX.Element | string | undefined;
  dataIndex?: number;
  key?: string;
  aggregate?: AggregateFieldActionType | undefined;
  colName?: string;
  width?: string | number;
  fixed?: string | null;
  ellipsis?: {
    showTitle: boolean;
  };
  onHeaderCell?: (record) => {
    [x: string]: any;
    uid?: string | undefined;
    onResize?: (e, node) => void;
  };
  onCell?: (record, rowIndex) => any;
  render?: (value, row, rowIndex) => any;
}

export type TableHeaderConfig = {
  children?: TableHeaderConfig[];
  isGroup?: boolean;
  label?: string;
  style?: {
    font?: {
      value?: FontStyle;
    };
    align?: string;
    backgroundColor?: string;
  };
} & ChartDataSectionField;

export interface TableComponentConfig {
  header: { cell: (props) => JSX.Element };
  body: {
    cell: (props) => JSX.Element;
    row: (props) => JSX.Element;
    wrapper: (props) => JSX.Element;
  };
}

export type PageOptions =
  | {
      showSizeChanger: boolean;
      current: number | undefined;
      pageSize: number | undefined;
      total: number | undefined;
    }
  | false;

export interface TableStyleOptions {
  scroll: {
    scrollToFirstRowOnChange: boolean;
    x: string | number;
    y: string | number;
  };
  bordered: boolean;
  size: string;
}

export type TableCellEvents =
  | {
      [key: string]: (e) => any;
    }
  | undefined;
