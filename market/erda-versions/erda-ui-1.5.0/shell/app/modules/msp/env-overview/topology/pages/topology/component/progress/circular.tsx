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
import { Progress } from 'antd';
import './index.scss';

interface IProps {
  className?: string;
  percent?: number;
  width?: number;
  stroke: string[];
  strokeWidth?: number;
}

const Circular: React.FC<IProps> = ({ width = 100, stroke, percent = 0, strokeWidth = 5, className, children }) => {
  const [bgColor, frontColor] = stroke;
  const wrapperWidth = width + strokeWidth;
  return (
    <div className="relative" style={{ width: wrapperWidth, height: wrapperWidth }}>
      <Progress
        width={width + strokeWidth}
        strokeWidth={strokeWidth}
        strokeColor={bgColor}
        percent={100}
        success={{ percent, strokeColor: frontColor }}
        type="circle"
        format={() => ''}
      />
      {children ? (
        <div className="absolute top-0 left-0" style={{ width: wrapperWidth, height: wrapperWidth }}>
          {children}
        </div>
      ) : null}
    </div>
  );
};

export default Circular;
