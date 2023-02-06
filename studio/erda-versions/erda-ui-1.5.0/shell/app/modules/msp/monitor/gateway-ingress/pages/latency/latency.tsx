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
import gatewayIngressCommonStore from 'app/modules/msp/monitor/gateway-ingress/stores/common';
import LatencyMap from './config/chartMap';
import TopTabRight from '../../common/components/tab-right';
import StatisticsSelector, { STATISTICS } from '../../common/components/statistics-selector';
import routeInfoStore from 'core/stores/route';
import HttpStatusInput from '../../common/components/http-status-input';
import mspStore from 'msp/stores/micro-service';

const type = 'traffic';
const filterStatistics = {
  overall: {
    avg: { avg: 'latency_mean' },
    max: { max: 'latency_max' },
  },
  upstream: {
    avg: { avg: 'upstream_latency_mean' },
    max: { max: 'upstream_latency_max' },
  },
};
const Latency = () => {
  const clusterName = mspStore.useStore((s) => s.clusterName);
  const [projectId, terminusKey] = routeInfoStore.useStore((s) => [s.params.projectId, s.params.terminusKey]);
  const chosenDomain = gatewayIngressCommonStore.useStore((s) => s.chosenDomain);
  const [chosenStatistics, setChosenStatistics] = React.useState(STATISTICS.avg.value);
  const [httpStatus, setHttpStatus] = React.useState();
  const query: any = { projectId, filter_cluster_name: clusterName };
  if (chosenDomain) {
    query.filter_req_host = chosenDomain;
  } else {
    query.filter_target_terminus_key = terminusKey;
  }
  if (httpStatus) query.filter_http_status_code = httpStatus;

  const changeStatistics = (val: string) => {
    setChosenStatistics(val);
  };

  const changeHttpStatus = (val: number) => {
    setHttpStatus(val);
  };
  return (
    <div>
      <TopTabRight type={type}>
        <StatisticsSelector onChange={changeStatistics} />
        <HttpStatusInput onChange={changeHttpStatus} />
      </TopTabRight>
      <Row gutter={20}>
        <Col span={12}>
          <LatencyMap.overall query={{ ...query, ...filterStatistics.overall[chosenStatistics] }} />
        </Col>
        <Col span={12}>
          <LatencyMap.upstream query={{ ...query, ...filterStatistics.upstream[chosenStatistics] }} />
        </Col>
      </Row>
    </div>
  );
};

export default Latency;
