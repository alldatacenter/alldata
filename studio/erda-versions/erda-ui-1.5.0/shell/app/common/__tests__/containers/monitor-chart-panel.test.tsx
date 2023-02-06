// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { mount } from 'enzyme';
import { MetricsMonitor } from 'common';
import metricsMonitorStore from 'common/stores/metrics';
import { act } from 'react-dom/test-utils';

const props = {
  resourceType: 'mySql',
  resourceId: '123',
  commonChartQuery: {
    filter_cluster_name: 'ERDA',
    filter_addon_id: 'addon-mysql',
    customAPIPrefix: '/api/addon/metrics/charts/',
  },
};

const metricsData = {
  listMetric: {
    [props.resourceType]: {
      addonA: {
        name: 'mysql',
      },
      addonB: {
        name: 'addon-mysql',
      },
    },
  },
  metricItem: {
    'mySql-123-mysql': {},
    'mySql-123-addon-mysql': {},
  },
};

describe('MetricsMonitor', () => {
  beforeAll(() => {
    jest.mock('common/stores/metrics');
    metricsMonitorStore.useStore = (fn) => {
      return fn(metricsData);
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('should render well', () => {
    const listMetricByResourceType = jest.fn();
    const clearListMetrics = jest.fn();
    metricsMonitorStore.effects.listMetricByResourceType = listMetricByResourceType;
    metricsMonitorStore.effects.loadMetricItem = jest.fn();
    metricsMonitorStore.reducers.clearListMetrics = clearListMetrics;
    jest.useFakeTimers();
    const wrapper = mount(<MetricsMonitor {...props} />);
    act(() => {
      jest.runAllTimers();
    });
    expect(listMetricByResourceType).toHaveBeenLastCalledWith({ resourceType: props.resourceType });
    wrapper.unmount();
    expect(clearListMetrics).toHaveBeenCalled();
  });
});
