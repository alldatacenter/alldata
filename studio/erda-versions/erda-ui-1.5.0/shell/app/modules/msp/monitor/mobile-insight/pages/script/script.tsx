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
import ScriptMap from './config/chartMap';
import './script.scss';
import { TimeSelectWithStore } from 'msp/components/time-select';

const Script = () => {
  const getAllChart = () => {
    return (
      <React.Fragment>
        <ScriptMap.errorTopN />
        <ScriptMap.osTopN />
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
            <ScriptMap.sortTab />
            <ScriptMap.sortList />
          </div>
        </Col>
        <Col span={16}>{getAllChart()}</Col>
      </Row>
    </div>
  );
};
export default Script;
