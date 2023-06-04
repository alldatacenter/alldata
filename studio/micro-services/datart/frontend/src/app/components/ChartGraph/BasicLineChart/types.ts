import {
  FormatFieldAction,
  LabelStyle,
  MarkArea,
  MarkLine,
} from 'app/types/ChartConfig';
export type Series = {
  data: Array<{
    rowData: { [key: string]: any };
    value: number | string | undefined;
    name?: string | undefined;
    format: FormatFieldAction | undefined;
  }>;
  smooth?: boolean;
  step?: boolean;
  stack?: string | undefined | boolean;
  connectNulls?: boolean;
  symbol?: string;
  name: string;
  type: string;
  sampling: string;
  areaStyle: { color?: string | undefined } | undefined;
  itemStyle: {
    color?: string | undefined;
    normal?: {
      color?: string | undefined;
    };
  };
  markLine?: MarkLine;
  markArea?: MarkArea;
} & LabelStyle;
