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

import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import { init } from 'echarts';
import Chart from '../../../models/Chart';
import Config from './config';

class BasicAreaChart extends Chart {
  dependency = [];
  config = Config;
  chart: any = null;
  option = {
    xAxis: {
      type: 'category',
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: [820, 932, 901, 934, 1290, 1330, 1320],
        type: 'line',
        areaStyle: {},
      },
    ],
  };

  constructor() {
    super('area', 'viz.palette.graph.names.areaChart', 'area-chart');
  }

  onMount(options: BrokerOption, context: BrokerContext) {
    if (options.containerId === undefined || !context.document) {
      return;
    }

    this.chart = init(
      context.document.getElementById(options.containerId)!,
      'default',
    );
  }

  onUpdated(options: BrokerOption, context: BrokerContext) {
    this.chart?.setOption(Object.assign({}, options?.config), true);
  }

  onUnMount(options: BrokerOption, context: BrokerContext) {
    this.chart?.dispose();
  }

  onResize(options: BrokerOption, context: BrokerContext) {
    this.chart?.resize(context);
  }
}

export default BasicAreaChart;
