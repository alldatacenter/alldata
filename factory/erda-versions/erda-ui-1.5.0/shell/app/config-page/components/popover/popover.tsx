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
import { Popover } from 'antd';
import './popover.scss';

const CP_POPOVER = (props: CP_POPOVER.Props) => {
  const sizeMap = {
    s: 200,
    m: 400,
    l: 600,
    xl: 900,
  };
  const { props: configProps, children, content } = props;
  const { visible = true, size, ...rest } = configProps || {};

  if (!visible) return null;
  const contentComp = <div style={{ width: (size && sizeMap[size]) || sizeMap.m }}>{content}</div>;
  return (
    <Popover overlayClassName={'cp-popover'} {...rest} content={contentComp}>
      <span>{children}</span>
    </Popover>
  );
};

export default CP_POPOVER;
