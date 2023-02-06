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
import { useMediaGt } from '../../use-hooks';

const suitableMap = {
  1: 24,
  2: 12,
  3: 8,
  4: 6,
  5: 6,
  6: 4,
  7: 4,
  8: 3,
  9: 3,
  10: 2,
};

interface IResponsive {
  children: Array<{ key: string }> | React.ReactChild;
  percent?: number;
  itemWidth: number;
  gutter?: number;
  align?: 'top' | 'middle' | 'bottom';
  justify?: 'start' | 'center' | 'end' | 'space-around' | 'space-between';
  type?: 'flex';
  className?: string;
}

// 内容区大于1024px时 main padding 为2*32
const extraPadding = 32;

/**
 * 响应式布局，以main区域宽度为标准
 * @param itemWidth 单个元素最小宽度
 * @param percent 容器宽度占总屏幕比例，默认为1
 * @param ...rest 透传到antd的Row组件上
 *
 * usage:
 * <Responsive itemWidth={300} percent={0.7}>
 *   {list}
 * <Responsive>
 */
const Responsive = ({
  children,
  itemWidth,
  percent = 1,
  gutter = 20,
  align,
  justify,
  type,
  className = '',
}: IResponsive) => {
  const item = itemWidth + gutter;
  const gt480 = useMediaGt((480 - gutter) / percent, true);
  const gt640 = useMediaGt((640 - gutter) / percent, true);
  const gt800 = useMediaGt((800 - gutter) / percent, true);
  const gt960 = useMediaGt((960 - gutter) / percent, true);
  const gt1120 = useMediaGt((1120 - extraPadding - gutter) / percent, true);
  const gt1280 = useMediaGt((1280 - extraPadding - gutter) / percent, true);
  const gt1440 = useMediaGt((1440 - extraPadding - gutter) / percent, true);
  const gt1600 = useMediaGt((1600 - extraPadding - gutter) / percent, true);
  const gt1920 = useMediaGt((1920 - extraPadding - gutter) / percent, true);
  let curWidth = item;
  if (gt1920) {
    curWidth = 1920;
  } else if (gt1600) {
    curWidth = 1600;
  } else if (gt1440) {
    curWidth = 1440;
  } else if (gt1280) {
    curWidth = 1280;
  } else if (gt1120) {
    curWidth = 1120;
  } else if (gt960) {
    curWidth = 960;
  } else if (gt800) {
    curWidth = 800;
  } else if (gt640) {
    curWidth = 640;
  } else if (gt480) {
    curWidth = 480;
  }
  // 每行几个
  const rowNum = Math.floor(curWidth / item);
  // 装换为24栅格中合适的比例
  const span = suitableMap[rowNum] || 1;

  return (
    <Row gutter={gutter} align={align} justify={justify} type={type} className={className}>
      {Array.isArray(children) ? (
        children.map((child) => {
          return (
            <Col key={child.key} span={span}>
              {child}
            </Col>
          );
        })
      ) : (
        <Col span={span}>{children}</Col>
      )}
    </Row>
  );
};

export default Responsive;
