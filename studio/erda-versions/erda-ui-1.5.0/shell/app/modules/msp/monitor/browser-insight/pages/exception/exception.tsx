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
import { TimeSelectWithStore } from 'msp/components/time-select';
import ExceptionMap from './config/chartMap';
import monitorCommonStore from 'common/stores/monitorCommon';
import './exception.scss';

const Exception = () => {
  const chosenSortItem = monitorCommonStore.useStore((s) => s.chosenSortItem);

  const getAllChart = () => {
    return <ExceptionMap.exception />;
  };
  const getDetailChart = () => {
    const key = chosenSortItem;
    return (
      <React.Fragment>
        <ExceptionMap.exception query={{ key }} />
        <ExceptionMap.slowTrack query={{ key }} />
      </React.Fragment>
    );
  };
  return (
    <div>
      <TimeSelectWithStore />
      <Row gutter={20}>
        <Col span={8}>
          <div className="monitor-sort-panel">
            <ExceptionMap.sortTab />
            <ExceptionMap.sortList />
          </div>
        </Col>
        <Col span={16}>{chosenSortItem ? getDetailChart() : getAllChart()}</Col>
      </Row>
    </div>
  );
};

export default Exception;
