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
import ReactChart from '../ReactChart';

jest.mock('react-dom', () => ({
  render: jest.fn(),
  unmountComponentAtNode: jest.fn(),
}));

jest.mock('react', () => ({
  createElement: (...args) => args,
}));

describe('ReactChart Tests', () => {
  test('should get correct model', () => {
    const mockWrapper = jest.fn();
    const chartMetaInfo = { id: 'react', name: 'react-chart', icon: 'chart' };
    const reactChart = new ReactChart(mockWrapper, chartMetaInfo);
    reactChart.onMount(
      { containerId: 1 },
      { document: { getElementById: jest.fn() } },
    );
    reactChart.onUnMount(null, null);
    expect(reactChart).not.toBeNull();
    expect(reactChart).toBeInstanceOf(ReactChart);
    expect(reactChart).toBeInstanceOf(Chart);
  });

  test('should get default values', () => {
    const mockWrapper = jest.fn();
    const chartMetaInfo = {};
    const reactChart = new ReactChart(mockWrapper, chartMetaInfo);
    expect(reactChart.meta.id).toEqual('react-table');
    expect(reactChart.meta.name).toEqual('表格');
    expect(reactChart.meta.icon).toEqual('table');
  });

  test('should get internal adapter', () => {
    const mockWrapper = jest.fn();
    const chartMetaInfo = { id: 'react', name: 'react-chart', icon: 'chart' };
    const reactChart = new ReactChart(mockWrapper, chartMetaInfo);
    expect(reactChart.adapter).not.toBeNull();
  });
});
