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
import { Spin } from 'antd';
import { find } from 'lodash';
import PureChartList from '../components/chart';
import projectResourceStore from 'project/stores/resource';
import { useLoading } from 'core/stores/loading';

const Chart = (props) => {
  const { getChartData } = projectResourceStore.effects;
  const chartList = projectResourceStore.useStore((s) => s.chartList);
  const data = find(chartList, (v) => v === undefined) ? [] : chartList;
  const [isFetching] = useLoading(projectResourceStore, ['getChartData']);

  const getChartList = React.useCallback(
    (payload = {}) => {
      getChartData({ type: 'cpu', ...payload });
      getChartData({ type: 'memory', ...payload });
    },
    [getChartData],
  );

  React.useEffect(() => {
    getChartList({ paths: props.paths, startLevel: props.startLevel });
  }, [getChartList, props.paths, props.startLevel, props.timeSpan]);
  return (
    <Spin spinning={isFetching}>
      <PureChartList {...props} data={data} />
    </Spin>
  );
};

export default Chart;
