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
import monitorCommonStore from 'common/stores/monitorCommon';
import SummaryMap from './config/chartMap';
import './summary.scss';

const Summary = () => {
  const [chosenSortItem, sortTab] = monitorCommonStore.useStore((s) => [s.chosenSortItem, s.sortTab]);
  let query = {};
  if (chosenSortItem) {
    const keyMap = {
      host: 'filter_host',
      url: 'filter_doc_path',
      os: 'filter_os',
      browser: 'filter_browser',
      device: 'filter_device',
    };
    const paramKey = keyMap[sortTab];
    query = { [paramKey]: chosenSortItem };
  }
  return (
    <div>
      <div className="flex justify-end mb-3">
        <TimeSelectWithStore />
      </div>{' '}
      <Row className="summary" gutter={20}>
        <Col span={8} style={{ minWidth: 500 }}>
          <div className="sort-panel">
            <SummaryMap.sortTab />
            <SummaryMap.sortList />
          </div>
        </Col>
        <Col span={16} className="bg-transparent flex-1">
          <SummaryMap.summaryDetail query={query} />
        </Col>
      </Row>
    </div>
  );
};
export default Summary;
