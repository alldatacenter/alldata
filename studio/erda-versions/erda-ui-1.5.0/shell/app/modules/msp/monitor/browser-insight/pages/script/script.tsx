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
import ScriptMap from './config/chartMap';

const Script = () => {
  const getAllChart = () => {
    return [<ScriptMap.errorTopN />, <ScriptMap.browsersTopN />];
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
        <Col span={16}>
          <Row gutter={[20, 20]}>
            {getAllChart().map((item) => (
              <Col span={24}>{item}</Col>
            ))}
          </Row>
        </Col>
      </Row>
    </div>
  );
};
export default Script;
