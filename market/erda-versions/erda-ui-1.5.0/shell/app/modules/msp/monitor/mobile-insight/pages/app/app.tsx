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
import AppMap from './config/chartMap';
import monitorCommonStore from 'common/stores/monitorCommon';
import { TimeSelectWithStore } from 'msp/components/time-select';

const App = () => {
  const chosenSortItem = monitorCommonStore.useStore((s) => s.chosenSortItem);
  const getAllChart = () => {
    return (
      <React.Fragment>
        <AppMap.performanceInterval />
        <AppMap.timeTopN />
        <AppMap.cpmTopN />
        {
          // <AppMap.slowTrack />
        }
      </React.Fragment>
    );
  };
  const getDetailChart = () => {
    const filter_av = chosenSortItem;
    const query = filter_av ? { filter_av } : {};
    return (
      <React.Fragment>
        <AppMap.performanceInterval query={query} />
        <AppMap.pagePerformanceTrends query={query} />
        {
          // <AppMap.slowTrack query={query} />
        }
      </React.Fragment>
    );
  };
  return (
    <div>
      <div className="flex justify-end mb-3">
        <TimeSelectWithStore />
      </div>
      <Row gutter={20}>
        <Col span={8}>
          <div className="monitor-sort-panel">
            <AppMap.sortTab />
            <AppMap.sortList />
          </div>
        </Col>
        <Col span={16}>{chosenSortItem ? getDetailChart() : getAllChart()}</Col>
      </Row>
    </div>
  );
};
export default App;
