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
import monitorCommonStore from 'common/stores/monitorCommon';
import DomainMap from './config/chartMap';
import { TimeSelectWithStore } from 'msp/components/time-select';
import './domain.scss';

const Domain = () => {
  const chosenSortItem = monitorCommonStore.useStore((s) => s.chosenSortItem);
  const getAllChart = () => {
    return [<DomainMap.performanceInterval />, <DomainMap.timeTopN />, <DomainMap.cpmTopN />];
  };

  const getDetailChart = () => {
    const filter_host = chosenSortItem;
    const query = filter_host ? { filter_host } : {};
    return [
      <DomainMap.performanceInterval query={query} />,
      <DomainMap.domainPerformanceTrends query={query} />,
      <DomainMap.pageTopN query={query} />,
      <DomainMap.slowTrack query={query} />,
    ];
  };

  return (
    <div>
      <div className="flex justify-between mb-3">
        <DomainMap.subTab />
        <TimeSelectWithStore />
      </div>
      <Row gutter={20}>
        <Col span={8}>
          <div className="monitor-sort-panel">
            <DomainMap.sortTab />
            <DomainMap.sortList />
          </div>
        </Col>
        <Col className="bi-domain-charts" span={16}>
          <Row gutter={[0, 20]}>
            {(chosenSortItem ? getDetailChart() : getAllChart()).map((item) => (
              <Col span={24}>{item}</Col>
            ))}
          </Row>
        </Col>
      </Row>
    </div>
  );
};

export default Domain;
