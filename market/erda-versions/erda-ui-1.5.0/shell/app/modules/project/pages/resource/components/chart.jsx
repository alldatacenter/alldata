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
import { compact } from 'lodash';
import { MonitorChartNew } from 'charts';
import { CardContainer } from 'common';
import { chartConfig } from './config';

const { ChartContainer } = CardContainer;

const Chart = (props) => {
  const { data, timeSpan } = props;

  return (
    <div className="chart-list">
      <Row gutter={20}>
        {compact(data).map((item) => {
          return (
            <Col key={item.title} span={12}>
              <ChartContainer title={item.title}>
                <MonitorChartNew {...chartConfig[item.title]} timeSpan={timeSpan} data={item} title={item.title} />
              </ChartContainer>
            </Col>
          );
        })}
      </Row>
    </div>
  );
};

export default Chart;
