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
import QPSMap from './config/chartMap';
import routeInfoStore from 'core/stores/route';
import TopTabRight from '../../common/components/tab-right';
import mspStore from 'msp/stores/micro-service';

const type = 'qps';
const QPS = () => {
  const clusterName = mspStore.useStore((s) => s.clusterName);
  const [projectId, terminusKey] = routeInfoStore.useStore((s) => [s.params.projectId, s.params.terminusKey]);
  const chosenDomain = gatewayIngressCommonStore.useStore((s) => s.chosenDomain);
  const query: any = { projectId, filter_cluster_name: clusterName };
  if (chosenDomain) {
    query.filter_req_host = chosenDomain;
  } else {
    query.filter_target_terminus_key = terminusKey;
  }
  return (
    <div>
      <TopTabRight type={type} />
      <Row gutter={20}>
        <Col span={12}>
          <QPSMap.overallQPS query={query} />
        </Col>
        <Col span={12}>
          <QPSMap.successQPS query={query} />
        </Col>
      </Row>
      <Row gutter={20}>
        <Col span={12}>
          <QPSMap.QPS4xx query={query} />
        </Col>
        <Col span={12}>
          <QPSMap.QPS5xx query={query} />
        </Col>
      </Row>
    </div>
  );
};

export default QPS;
