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

import ReactChart from 'app/models/ReactChart';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import Config from './config';
import ReactXYPlot from './ReactVizXYPlot';

class ReactVizXYPlotChart extends ReactChart {
  isISOContainer = 'reactviz-container';
  config = Config;
  dependency = [
    'https://unpkg.com/react-vis/dist/style.css',
    'https://unpkg.com/react-vis/dist/dist.min.js',
  ];

  constructor() {
    super(ReactXYPlot, {
      id: 'reactviz-xyplot-chart',
      name: 'ReactViz XYPlot Chart',
      icon: 'star',
    });
  }

  onMount(options: BrokerOption, context: BrokerContext) {
    if (!context.window['reactVis']) {
      return;
    }
    const { XYPlot, XAxis, YAxis, HorizontalGridLines, LineSeries } =
      context.window['reactVis'];
    this.adapter.init(ReactXYPlot);
    this.adapter.registerImportDependencies({
      XYPlot,
      XAxis,
      YAxis,
      HorizontalGridLines,
      LineSeries,
    });
    this.adapter.mounted(context.document.getElementById(options.containerId));
  }

  onUpdated(options: BrokerOption, context: BrokerContext): void {
    // this.adapter.updated(props);
  }
}

export default ReactVizXYPlotChart;
