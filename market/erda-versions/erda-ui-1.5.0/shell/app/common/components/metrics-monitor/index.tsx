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
import { Holder } from 'common';
import { isEmpty } from 'lodash';
import PureMonitorChartPanel from 'common/components/monitor/monitor-chart-panel';
import metricsMonitorStore from 'common/stores/metrics';
import { useUnmount } from 'react-use';
// import routeInfoStore from 'core/stores/route';

interface IProps {
  resourceType: string;
  fetchApi: string;
  resourceId: string;
  listMetric: any;
  params: any;

  initFetch: (...args: any) => Promise<any>;
}

const MonitorChartPanel = (props: IProps) => {
  // const params = routeInfoStore.useStore(s => s.params);
  const { resourceType } = props;
  const [listMetric] = metricsMonitorStore.useStore((s) => [s.listMetric]);
  const { listMetricByResourceType: initFetch } = metricsMonitorStore.effects;
  const { clearListMetrics } = metricsMonitorStore.reducers;
  const [data, setData] = React.useState({});
  React.useEffect(() => {
    initFetch({ resourceType });
  }, [initFetch, resourceType]);

  useUnmount(() => {
    clearListMetrics();
  });

  React.useEffect(() => {
    const metrics = listMetric[resourceType] || {};
    const list = Object.keys(metrics).map((key) => ({ key, value: metrics[key] }));
    const lazyLoad = () => {
      if (list.length) {
        setTimeout(() => {
          const { key, value } = list.shift() || {};
          key && setData((prevData) => ({ ...prevData, [key]: value }));
          lazyLoad();
        }, 200);
      }
    };
    lazyLoad();
  }, [listMetric, resourceType]);

  return (
    <Holder when={isEmpty(data)}>
      <PureMonitorChartPanel {...props} metrics={data} />
    </Holder>
  );
};

export default MonitorChartPanel;
