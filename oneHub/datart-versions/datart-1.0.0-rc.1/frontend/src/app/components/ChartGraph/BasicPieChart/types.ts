import {
  ChartDataSectionField,
  FormatFieldAction,
  GridStyle,
  LabelStyle,
} from 'app/types/ChartConfig';

export interface PieSeries {
  radius: string[] | string;
  roseType: boolean;
}

export type PieSeriesImpl = {
  type: string;
  sampling: string;
  avoidLabelOverlap: boolean;
} & GridStyle &
  PieSeries &
  LabelStyle;

export type PieSeriesStyle = {
  name?: string;
  data: Array<
    {
      format: FormatFieldAction | undefined;
      name: string;
      value: any[];
      itemStyle: { [x: string]: any } | undefined;
      rowData: { [key: string]: any };
    } & ChartDataSectionField
  >;
} & PieSeriesImpl;
