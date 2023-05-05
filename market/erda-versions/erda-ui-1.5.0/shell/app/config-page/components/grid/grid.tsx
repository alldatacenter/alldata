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
import { Col, Row } from 'antd';

const CP_GRID = (props: CP_GRID.Props) => {
  const { props: configProps, children } = props;
  const { gutter = 12, span } = configProps || {};
  let itemSpan = span;
  if (!itemSpan) {
    itemSpan = new Array(children.length).fill(Math.ceil(24 / children.length));
  }
  return (
    <div className="overflow-hidden">
      <Row gutter={gutter}>
        {children.map((child, i) => (
          <Col span={itemSpan[i]} key={i} className='overflow-auto'>
            {child}
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default CP_GRID;
