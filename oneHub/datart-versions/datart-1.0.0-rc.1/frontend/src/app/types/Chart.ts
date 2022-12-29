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

import { ChartInteractionEvent } from 'app/constants';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { ChartConfig } from './ChartConfig';
import { BrokerContext, BrokerOption } from './ChartLifecycleBroker';
import ChartMetadata from './ChartMetadata';

export type ChartStatus =
  | 'init'
  | 'depsLoaded'
  | 'ready'
  | 'mounting'
  | 'updating'
  | 'unmounting'
  | 'error';

export interface ChartMouseEvent {
  name:
    | 'click'
    | 'dblclick'
    | 'mousedown'
    | 'mousemove'
    | 'mouseup'
    | 'mouseover'
    | 'mouseout'
    | 'globalout'
    | 'contextmenu';
  callback: (params?: ChartMouseEventParams) => void;
}

export interface ChartsEventData extends Object {
  name: string;
  value: string;
  rowData: { [propName: string]: any };
}

// Note: `EventParams` type from echarts definition. But no interaction types are included.
export interface ChartMouseEventParams {
  // 图标类型 'table', 'pivotSheet', 'bar',
  chartType?: string;
  // 交互类型 'select', 'drilled', 'paging-sort-filter', 'rich-text-change-context'
  interactionType?: ChartInteractionEvent;

  // 当前点击的图形元素所属的组件名称，
  // 其值如 'series'、'markLine'、'markPoint'、'timeLine' 等。
  componentType?: string;
  // 系列类型。值可能为：'line'、'bar'、'pie' 等。当 componentType 为 'series' 时有意义。
  seriesType?: string;
  // 系列在传入的 option.series 中的 index。当 componentType 为 'series' 时有意义。
  seriesIndex?: number;
  // 系列名称。当 componentType 为 'series' 时有意义。
  seriesName?: string;
  // 数据名，类目名
  name?: string;
  // 数据在传入的 data 数组中的 index
  dataIndex?: number;
  // 传入的原始数据项
  data?: ChartsEventData;
  // sankey、graph 等图表同时含有 nodeData 和 edgeData 两种 data，
  // dataType 的值会是 'node' 或者 'edge'，表示当前点击在 node 还是 edge 上。
  // 其他大部分图表中只有一种 data，dataType 无意义。
  dataType?: string;
  // 传入的数据值
  value?: number | string | [] | object | any;
  // 数据图形的颜色。当 componentType 为 'series' 时有意义。
  color?: string;
  [p: string]: any;
}

export interface IChartLifecycle {
  /**
   * Mount event with params `option` and `context`
   *
   * @abstract
   * @param {BrokerOption} options
   * @param {BrokerContext} context
   * @memberof DatartChartBase
   */
  onMount(options: BrokerOption, context: BrokerContext): void;

  /**
   * Update event with params `option` and `context`
   *
   * @abstract
   * @param {BrokerOption} options
   * @param {BrokerContext} context
   * @memberof DatartChartBase
   */
  onUpdated(options: BrokerOption, context: BrokerContext): void;

  /**
   * UnMount event with params `option` and `context`
   *
   * @abstract
   * @param {BrokerOption} options
   * @param {BrokerContext} context
   * @memberof DatartChartBase
   */
  onUnMount(options: BrokerOption, context: BrokerContext): void;

  /**
   * Resize event with params `option` and `context`
   *
   * @abstract
   * @param {BrokerOption} options
   * @param {BrokerContext} context
   * @memberof DatartChartBase
   */
  onResize(options: BrokerOption, context: BrokerContext): void;
}

export interface IChart extends IChartLifecycle {
  meta: ChartMetadata;
  config?: ChartConfig;
  dataset?: ChartDataSetDTO;
  dependency: string[];
  isISOContainer: boolean | string;
  useIFrame?: boolean;

  set state(state: ChartStatus);
  get state();

  getDependencies(): string[];

  init(config: any);
  registerMouseEvents(events: Array<ChartMouseEvent>);
  isMatchRequirement(targetConfig?: ChartConfig): boolean;
}
