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

import { Progress } from 'antd';
import { floor } from 'lodash';
import React from 'react';

import './index.scss';

const CircleProgress = ({ left = 0, right = 0, autoFontSize = true, className = '', ...rest }) => {
  let fontSize = '';
  if (autoFontSize) {
    fontSize = `${left}${right}`.length > 5 ? 'small-font' : '';
  }

  return (
    <Progress
      className={`circle-progress ${fontSize} ${className}`}
      width={140}
      strokeWidth={7}
      type="circle"
      format={() => `${left}/${right}`}
      percent={right === 0 ? 0 : floor((left / right) * 100, 2)}
      {...rest}
    />
  );
};

export default CircleProgress;
