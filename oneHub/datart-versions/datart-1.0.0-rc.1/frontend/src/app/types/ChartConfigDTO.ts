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

import { ChartDataConfig } from 'app/types/ChartConfig';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { ECharts } from 'echarts';

export type ChartStyleConfigDTO = {
  key: string;
  value?: any;
  rows?: ChartStyleConfigDTO[];
  comType?: string;
  disabled?: boolean;
};

export type ChartDataConfigDTO = ChartDataConfig & {};

export type ChartConfigDTO = {
  datas?: ChartDataConfigDTO[];
  styles?: ChartStyleConfigDTO[];
  settings?: ChartStyleConfigDTO[];
  interactions?: ChartStyleConfigDTO[];
};

export type ChartDetailConfigDTO = {
  chartConfig: ChartConfigDTO;
  chartGraphId: string;
  computedFields: ChartDataViewMeta[];
  aggregation: boolean;
  sampleData?: any;
};

export interface ChartCommonConfig {
  chart: ECharts | null;
  // 官方ts定义过于复杂 所以用any
  // x轴options
  xAxis: any;
  // y轴options
  yAxis: any;
  // 布局
  grid: any;
  // y轴 字段索引名
  yAxisNames: string[];
  // 图表系列
  series: any[];
  // 水平还是垂直
  horizon?: boolean;
}
