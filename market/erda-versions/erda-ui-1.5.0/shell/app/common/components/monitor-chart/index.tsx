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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import PureMonitorChart from 'common/components/monitor/monitor-chart';
import monitorCommonStore from 'common/stores/monitorCommon';
import metricsMonitorStore from 'common/stores/metrics';

interface IProps {
  resourceType: string;
  resourceId: string;
  metricKey: string;
  chartQuery: any;
}

const MonitorChart = (props: IProps) => {
  const { resourceType, resourceId, metricKey, chartQuery } = props;
  const metricData = metricsMonitorStore.useStore((s) => s.metricItem);
  const initFetch = metricsMonitorStore.effects.loadMetricItem;
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);

  React.useEffect(() => {
    loadMetricData();
  }, [timeSpan]);

  const loadMetricData = (query: any = {}) => {
    const queryParams = {
      type: metricKey,
      start: timeSpan.startTimeMs,
      end: timeSpan.endTimeMs,
      resourceType,
      resourceId,
      chartQuery,
      ...query,
    };
    initFetch(queryParams);
  };

  const metricDataId = `${resourceType}-${resourceId}-${metricKey}`;
  return <PureMonitorChart {...props} data={metricData[metricDataId]} />;
};

export default MonitorChart;
