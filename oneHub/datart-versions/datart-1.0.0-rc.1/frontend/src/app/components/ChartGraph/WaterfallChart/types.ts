import { BorderStyle } from 'app/types/ChartConfig';

export type WaterfallBorderStyle = {
  borderRadius?: number;
} & BorderStyle;

export type OrderConfig =
  | number
  | string
  | {
      value: number;
      itemStyle?: {
        color?: string;
        opacity?: number;
      };
    };

export interface WaterfallDataListConfig {
  baseData: Array<number | string>;
  ascendOrder: OrderConfig[];
  descendOrder: OrderConfig[];
}
