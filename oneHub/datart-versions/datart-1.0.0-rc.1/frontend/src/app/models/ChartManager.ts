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
  AreaChart,
  BasicDoubleYChart,
  BasicFunnelChart,
  BasicGaugeChart,
  BasicRichText,
  BasicScatterChart,
  ClusterBarChart,
  ClusterColumnChart,
  DoughnutChart,
  LineChart,
  MingXiTableChart,
  NormalOutlineMapChart,
  PercentageStackBarChart,
  PercentageStackColumnChart,
  PieChart,
  PivotSheetChart,
  RoseChart,
  ScatterOutlineMapChart,
  Scorecard,
  StackAreaChart,
  StackBarChart,
  StackColumnChart,
  WaterfallChart,
  WordCloudChart,
} from 'app/components/ChartGraph';
import { IChart } from 'app/types/Chart';
import { getChartPluginPaths } from 'app/utils/fetch';
import { Debugger } from 'utils/debugger';
import { CloneValueDeep } from 'utils/object';
import PluginChartLoader from './PluginChartLoader';

class ChartManager {
  private _loader = new PluginChartLoader();
  private _isLoaded = false;
  private _charts: IChart[] = this._basicCharts();
  private static _manager: ChartManager | null = null;

  public static instance() {
    if (!this._manager) {
      this._manager = new ChartManager();
    }
    return this._manager;
  }

  public async load() {
    if (this._isLoaded) {
      return;
    }
    const pluginsPaths = await getChartPluginPaths();
    return Debugger.instance.measure('Plugin Charts | ', async () => {
      await this._loadCustomizeCharts(pluginsPaths);
    });
  }

  public getAllCharts(): IChart[] {
    return this._charts || [];
  }

  public getAllChartIcons() {
    return this._charts.reduce((acc, cur) => {
      acc[cur.meta.id] = cur.meta.icon;
      return acc;
    }, {});
  }

  public getById(id?: string) {
    if (id === null || id === undefined) {
      return;
    }
    return CloneValueDeep(this._charts.find(c => c.meta?.id === id));
  }

  public getDefaultChart() {
    return CloneValueDeep(this._charts[0]);
  }

  private async _loadCustomizeCharts(paths: string[]) {
    if (this._isLoaded) {
      return this._charts;
    }

    const customCharts = await this._loader.loadPlugins(paths);
    this._charts = this._charts.concat(
      customCharts?.filter(Boolean) as IChart[],
    );
    this._isLoaded = true;
    return this._charts;
  }

  private _basicCharts(): IChart[] {
    return [
      new MingXiTableChart(),
      new PivotSheetChart(),
      new Scorecard(),
      new ClusterColumnChart(),
      new ClusterBarChart(),
      new StackColumnChart(),
      new StackBarChart(),
      new PercentageStackColumnChart(),
      new PercentageStackBarChart(),
      new WaterfallChart(),
      new LineChart(),
      new AreaChart(),
      new StackAreaChart(),
      new BasicScatterChart(),
      new PieChart(),
      new DoughnutChart(),
      new RoseChart(),
      new BasicFunnelChart(),
      new BasicDoubleYChart(),
      new WordCloudChart(),
      new NormalOutlineMapChart(),
      new ScatterOutlineMapChart(),
      new BasicGaugeChart(),
      new BasicRichText(),
    ];
  }
}

export default ChartManager;
