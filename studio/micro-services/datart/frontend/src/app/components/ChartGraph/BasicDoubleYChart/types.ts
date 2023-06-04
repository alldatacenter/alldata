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
  AxisLabel,
  AxisLineStyle,
  ChartDataSectionField,
  FontStyle,
  FormatFieldAction,
  LabelStyle,
  LineStyle,
  MarkArea,
  MarkLine,
} from 'app/types/ChartConfig';

export type DoubleYChartXAxis = {
  axisPointer?: {
    show: boolean;
    type: string;
  };
  axisLabel: AxisLabel;
  axisLine: AxisLineStyle;
  axisTick: AxisLineStyle;
  data: string[];
  inverse: boolean;
  splitLine: AxisLineStyle;
  tooltip: { show: boolean };
  type: string;
};

export type DoubleYChartYAxis = {
  axisLabel: AxisLabel;
  axisLine: AxisLineStyle;
  inverse: undefined | boolean;
  name: string;
  nameGap: number;
  nameLocation: string;
  nameRotate: number;
  nameTextStyle: FontStyle;
  position: string;
  showTitleAndUnit: boolean;
  splitLine: AxisLineStyle;
  type: string;
  min?: number | string;
  max?: number | string;
  interval?: number;
};

export type Series = {
  yAxisIndex: number;
  name: string;
  data: Array<
    {
      rowData: { [key: string]: any };
      value: number | string;
      total?: number;
      format: FormatFieldAction | undefined;
    } & ChartDataSectionField
  >;
  color?: string;
  smooth?: boolean | undefined;
  stack?: boolean | undefined;
  step?: boolean | undefined;
  symbol?: string | undefined;
  itemStyle: { color: string | undefined };
  type: string;
  sampling: string;
  barWidth?: number | string;
  lineStyle?: LineStyle;
  markLine: MarkLine;
  markArea: MarkArea;
} & LabelStyle;

export interface IntervalConfig {
  leftMin?: number;
  leftMax?: number;
  rightMin?: number;
  rightMax?: number;
  leftInterval?: number;
  rightInterval?: number;
}
