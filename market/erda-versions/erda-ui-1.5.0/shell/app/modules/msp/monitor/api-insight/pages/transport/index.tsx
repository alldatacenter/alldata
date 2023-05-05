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
import FilterNav from 'app/modules/msp/monitor/api-insight/common/components/filterNav';
import TransportMap from './config/chartMap';
import routeInfoStore from 'core/stores/route';
import apiMonitorFilterStore from '../../stores/filter';
import gatewayStore from 'msp/stores/gateway';

const APITransport = () => {
  const { clusterName } = gatewayStore.useStore((s) => s.consumer);
  const { getConsumer } = gatewayStore.effects;
  const { projectId, env } = routeInfoStore.useStore((s) => s.params);
  const searchFields = apiMonitorFilterStore.useStore((s) => s.searchFields);
  const { resetSearchFields } = apiMonitorFilterStore.reducers;
  const { getSearchFields } = apiMonitorFilterStore.effects;

  const filter_dpid = projectId;
  const filter_denv = env.toLowerCase();
  const filter_cluster_name = clusterName;

  const shouldLoad = !!(filter_dpid && filter_denv && filter_cluster_name);

  const commonFilter = {
    filter_dpid,
    filter_denv,
    filter_cluster_name,
    projectId,
  };

  const [fields, setFields] = React.useState(searchFields);

  const resetFields = () => {
    resetSearchFields();
    setFields({});
  };

  const updateFields = () => {
    getSearchFields().then((filterFields: any) => {
      setFields(filterFields);
    });
  };

  React.useEffect(() => {
    getConsumer();
  }, [getConsumer]);

  return (
    <>
      <FilterNav updateFields={updateFields} resetFields={resetFields} isNeedStatusFilters={false} />
      <Row gutter={20}>
        <Col span={12}>
          <TransportMap.requestSize
            shouldLoad={shouldLoad}
            query={{
              ...commonFilter,
              ...fields,
            }}
          />
        </Col>
        <Col span={12}>
          <TransportMap.requestSizeTop
            shouldLoad={shouldLoad}
            query={{
              ...commonFilter,
              ...fields,
            }}
          />
        </Col>
      </Row>
      <Row gutter={20}>
        <Col span={12}>
          <TransportMap.responseSize
            shouldLoad={shouldLoad}
            query={{
              ...commonFilter,
              ...fields,
            }}
          />
        </Col>
        <Col span={12}>
          <TransportMap.responseSizeTop
            shouldLoad={shouldLoad}
            query={{
              ...commonFilter,
              ...fields,
            }}
          />
        </Col>
      </Row>
    </>
  );
};

export default APITransport;
