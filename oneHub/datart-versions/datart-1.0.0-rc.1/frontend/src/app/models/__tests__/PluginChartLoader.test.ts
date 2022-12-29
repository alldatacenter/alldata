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

import Chart from '../Chart';
import PluginChartLoader from '../PluginChartLoader';

jest.mock('app/utils/fetch', () => ({
  fetchPluginChart: path => {
    if (path === 'not-exist-chart') {
      return null;
    }
    if (/.iife.js$/.test(path)) {
      return `(function a() {
                "use strict";
            
                function e() {
                return {
                    config: {
                    datas: [],
                    styles: [],
                    settings: [],
                    i18ns: [],
                    },
                    isISOContainer: "iife-chart-container-id",
                    dependency: ["d3.min.js"],
                    meta: {
                    id: 1,
                    name: '${path}',
                    icon: "chart",
                    requirements: [],
                    }
                };
                }
                return e;
            })();`;
    }
    return `function chart() {
                return {
                    isISOContainer: 'pure-chart-container-id',
                    dependency: ['echarts.min.js'],
                    config: [],
                    meta: {
                        id: 1,
                        name: '${path}',
                        icon: 'chart',
                        requirements: []
                    }
                }
            }`;
  },
}));

describe('PluginChartLoader Tests', () => {
  test('should get correct loader', async () => {
    const loader = new PluginChartLoader();
    expect(loader).not.toBeNull();
  });

  test('should load pure js plugin charts', async () => {
    const loader = new PluginChartLoader();
    const charts = await loader.loadPlugins(['b-chart.js']);
    expect((charts[0] as Chart).meta).toEqual({
      id: 1,
      icon: 'chart',
      name: 'b-chart.js',
      requirements: [],
    });
    expect((charts[0] as Chart).config).toEqual([]);
    expect((charts[0] as Chart).dependency).toEqual(['echarts.min.js']);
    expect((charts[0] as Chart).isISOContainer).toEqual(
      'pure-chart-container-id',
    );
  });

  test('should load iife js plugin charts', async () => {
    const loader = new PluginChartLoader();
    const charts = await loader.loadPlugins(['a-chart.iife.js']);
    expect((charts[0] as Chart).meta).toEqual({
      id: 1,
      icon: 'chart',
      name: 'a-chart.iife.js',
      requirements: [],
    });
    expect((charts[0] as Chart).config).toEqual({
      datas: [],
      i18ns: [],
      settings: [],
      styles: [],
    });
    expect((charts[0] as Chart).dependency).toEqual(['d3.min.js']);
    expect((charts[0] as Chart).isISOContainer).toEqual(
      'iife-chart-container-id',
    );
  });

  test('should get reject promise when did not get charts', async () => {
    const loader = new PluginChartLoader();
    const charts = await loader.loadPlugins(['not-exist-chart']);
    expect(charts[0] as Chart).toEqual(null);
  });
});
