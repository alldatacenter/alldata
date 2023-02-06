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
import { Row, Col } from 'antd';
import ConnectionMap from './config/chartMap';
import TopTabRight from '../../common/components/tab-right';
import routeInfoStore from 'core/stores/route';
import StatisticsSelector, { STATISTICS } from '../../common/components/statistics-selector';
import mspStore from 'msp/stores/micro-service';

const type = 'connection';
const filterStatistics = {
  active: {
    avg: { avg: 'active_conns_mean' },
    max: { max: 'active_conns_max' },
  },
  idle: {
    avg: { avg: 'idle_conns_mean' },
    max: { max: 'idle_conns_max' },
  },
  total: {
    avg: { avg: 'total_conns_mean' },
    max: { max: 'total_conns_max' },
  },
};
const Connection = () => {
  const clusterName = mspStore.useStore((s) => s.clusterName);
  const projectId = routeInfoStore.useStore((s) => s.params.projectId);
  const [chosenStatistics, setChosenStatistics] = React.useState(STATISTICS.avg.value);
  const query: any = { projectId, filter_cluster_name: clusterName };

  const changeStatistics = (val: string) => {
    setChosenStatistics(val);
  };

  return (
    <div>
      <TopTabRight type={type}>
        <StatisticsSelector onChange={changeStatistics} />
      </TopTabRight>
      <Row gutter={20}>
        <Col span={12}>
          <ConnectionMap.activeConnect query={{ ...query, ...filterStatistics.active[chosenStatistics] }} />
        </Col>
        <Col span={12}>
          <ConnectionMap.idleConnect query={{ ...query, ...filterStatistics.idle[chosenStatistics] }} />
        </Col>
      </Row>
      <Row gutter={20}>
        <Col span={12}>
          <ConnectionMap.totalConnect query={{ ...query, ...filterStatistics.total[chosenStatistics] }} />
        </Col>
      </Row>
    </div>
  );
};

export default Connection;
