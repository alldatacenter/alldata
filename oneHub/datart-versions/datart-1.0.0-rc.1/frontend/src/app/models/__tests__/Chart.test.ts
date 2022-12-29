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

describe('Chart Tests', () => {
  test('should get correct chart model', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chartFontIcon = 'bar-chart';
    const chartRequirements = [] as any;
    const chart = new Chart(
      chartId,
      chartName,
      chartFontIcon,
      chartRequirements,
    );
    expect(chart.meta.id).toEqual(chartId);
    expect(chart.meta.name).toEqual(chartName);
    expect(chart.meta.icon).toEqual(chartFontIcon);
    expect(chart.meta.requirements).toEqual(chartRequirements);
    expect(chart.state).toEqual('init');
    expect(chart.onMount).toBeDefined();
    expect(chart.onUpdated).toBeDefined();
    expect(chart.onUnMount).toBeDefined();
    expect(chart.onResize).toBeDefined();
    expect(chart.isISOContainer).toBeFalsy();
    expect(chart.useIFrame).toBeTruthy();
    expect(chart.getDependencies()).toEqual([]);
  });

  test('should throw exception if method not implement', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chart = new Chart(chartId, chartName);
    expect(() => chart.onMount({} as any, {} as any)).toThrow(
      /onMount Method Not Implemented/,
    );
    expect(() => chart.onUpdated({} as any, {} as any)).toThrow(
      /onUpdated Method Not Implemented/,
    );
    expect(() => chart.onUnMount({} as any, {} as any)).toThrow(
      /onUnMount Method Not Implemented/,
    );
    expect(() => chart.onResize({} as any, {} as any)).not.toThrow(
      /Method not implemented/,
    );
  });

  test('should change chart state', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chart = new Chart(chartId, chartName);
    chart.state = 'ready';
    expect(chart.state).toEqual('ready');
  });

  test('should init chart config', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chartConfig = {};
    const chart = new Chart(chartId, chartName);
    chart.init(chartConfig);
    expect(chart.config).toEqual(chartConfig);
  });

  test('should register mouse events', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const mouseEvents = [];
    const chart = new Chart(chartId, chartName);
    chart.registerMouseEvents(mouseEvents);
    expect(chart.mouseEvents).toEqual(mouseEvents);
  });

  test('should always match requirements if target config is empty', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    let targetConfig;
    const chart = new Chart(chartId, chartName);
    expect(chart.isMatchRequirement(targetConfig)).toBeTruthy();
  });

  test('should match chart config section limitation when section type and key exactly matched', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chartConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimension',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }],
        },
      ],
    };
    let targetConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimension',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }],
        },
      ],
    };
    const chart = new Chart(chartId, chartName);
    chart.init(chartConfig);
    expect(chart.isMatchRequirement(targetConfig as any)).toBeTruthy();
  });

  test('should not match chart config section limitation when limit is not in range', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chartConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimension',
          required: true,
          type: 'group',
          limit: 2,
          rows: [{ id: 1 }],
        },
      ],
    };
    let targetConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimension',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }],
        },
      ],
    };
    const chart = new Chart(chartId, chartName);
    chart.init(chartConfig);
    expect(chart.isMatchRequirement(targetConfig as any)).toBeFalsy();
  });

  test('should match chart config section limitation when multi key and type matched', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chartConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimension',
          required: true,
          type: 'group',
          limit: 2,
          rows: [{ id: 1 }],
        },
      ],
    };
    let targetConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimensionL',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }],
        },
        {
          label: 'dimension',
          key: 'dimensionR',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }],
        },
      ],
    };
    const chart = new Chart(chartId, chartName);
    chart.init(chartConfig);
    expect(chart.isMatchRequirement(targetConfig as any)).toBeTruthy();
  });

  test('should match chart config section limitation when exactly matched target config section key', () => {
    const chartId = 'some chart id';
    const chartName = 'chart name';
    const chartConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimensionL',
          required: true,
          type: 'group',
          limit: 2,
          rows: [{ id: 1 }],
        },
      ],
    };
    let targetConfig = {
      datas: [
        {
          label: 'dimension',
          key: 'dimensionL',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }, { id: 2 }],
        },
        {
          label: 'dimension',
          key: 'dimensionR',
          required: true,
          type: 'group',
          limit: 1,
          rows: [{ id: 1 }],
        },
      ],
    };
    const chart = new Chart(chartId, chartName);
    chart.init(chartConfig);
    expect(chart.isMatchRequirement(targetConfig as any)).toBeTruthy();
  });
});
