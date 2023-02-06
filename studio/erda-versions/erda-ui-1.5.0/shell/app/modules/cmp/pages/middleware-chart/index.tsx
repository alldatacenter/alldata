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
import { useMount } from 'react-use';
import { map, isEmpty, get, forEach, mapKeys } from 'lodash';
import moment from 'moment';
import { BoardGrid } from 'common';
import { Spin } from 'antd';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import middlewareChartStore from '../../stores/middleware-chart';

const MiddlewareChart = () => {
  const { addon_id, timestamp, cluster_name } = routeInfoStore.useStore((s) => s.query);
  const [containerChartMetas, middlewareChartMetas] = middlewareChartStore.useStore((s) => [
    s.containerChartMetas,
    s.middlewareChartMetas,
  ]);
  const { getChartMeta, getContainerChart } = middlewareChartStore.effects;
  const [loading] = useLoading(middlewareChartStore, ['getChartMeta']);
  const timestampMap = React.useMemo(() => {
    const curTimestamp = moment().valueOf();
    const _timestampMap = { start: curTimestamp - 30 * 60 * 1000, end: curTimestamp };
    if (Number(timestamp) + 30 * 60 * 1000 < curTimestamp) {
      _timestampMap.start = Number(timestamp) - 30 * 60 * 1000;
      _timestampMap.end = Number(timestamp) + 30 * 60 * 1000;
    }
    return _timestampMap;
  }, [timestamp]);

  useMount(() => {
    // 容器图表元数据
    getChartMeta({ type: 'addon_container' });
    // 中间件图表元数据
    getChartMeta({ type: addon_id });
  });

  const getLayout = (chartMetas: any) =>
    map(chartMetas, ({ title, name, parameters }, index) => ({
      w: 12,
      h: 9,
      x: 12 * (index % 2),
      y: 0,
      i: `middleware-chart-${name}`,
      moved: false,
      static: false,
      view: {
        title,
        chartType: 'chart:line',
        hideReload: true,
        chartQuery: {
          start: timestampMap.start,
          end: timestampMap.end,
          filter_cluster_name: cluster_name,
          filter_addon_id: addon_id,
          name,
          ...parameters,
        },
        loadData: getContainerChart,
        dataConvertor(responseData: any) {
          if (isEmpty(responseData)) return {};
          const { time = [], results = [] } = responseData || {};
          const data = get(results, '[0].data') || [];
          const metricData = [] as object[];
          const yAxis = [];
          forEach(data, (item) => {
            mapKeys(item, (v) => {
              const { chartType, ...rest } = v;
              yAxis[v.axisIndex] = 1;
              metricData.push({
                ...rest,
                type: chartType || 'line',
              });
            });
          });
          const yAxisLength = yAxis.length;
          const formatTime = time.map((t) => moment(t).format('MM-DD HH:mm'));
          return { xData: formatTime, metricData, yAxisLength, xAxisIsTime: true };
        },
      },
    }));

  return (
    <Spin spinning={loading}>
      {/* <h3 className="title mb-4">{i18n.t('cmp:middleware container chart')}</h3> */}
      <BoardGrid.Pure layout={getLayout([...containerChartMetas, ...middlewareChartMetas])} />
      {/* <h3 className="title mt-6 mb-4">{i18n.t('cmp:middleware indicator chart')}</h3>
      <BoardGrid.Pure
        layout={getLayout(middlewareChartMetas)}
      /> */}
    </Spin>
  );
};

export default MiddlewareChart;
