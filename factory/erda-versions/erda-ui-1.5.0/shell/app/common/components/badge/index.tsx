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
import { Tooltip } from 'antd';
import { shuffle } from 'lodash';
import { colorToRgb } from 'common/utils';
import './index.scss';

export interface IBadgeProps {
  color?: string;
  tip?: string;
  status?: 'success' | 'error' | 'warning' | 'default' | 'processing';
  text: string;
  size?: 'small' | 'default';
  breathing?: boolean;
  className?: string;
  showDot?: boolean;
}

enum BadgeStatus {
  success = 'success',
  error = 'error',
  warning = 'warning',
  default = 'default',
  processing = 'processing',
}

const Badge = (props: IBadgeProps) => {
  const {
    color,
    tip,
    status = 'default',
    text,
    size = 'default',
    breathing: pBreathing,
    className = '',
    showDot = true,
  } = props;
  const defaultBreath = { processing: true };
  const breathing = pBreathing === undefined && status ? defaultBreath[status] : pBreathing || false;

  const colorStyle = color ? { color, backgroundColor: colorToRgb(color, 0.1) } : undefined;

  const breathCls = breathing ? `badge-breathing badge-breathing-${shuffle([1, 2, 3])[0]}` : '';

  return (
    <Tooltip title={tip}>
      <span
        style={colorStyle}
        className={`erda-badge erda-badge-status-${status} ${breathCls} badge-${size} inline-flex items-center rounded-sm ${className}`}
      >
        {showDot ? (
          <span className="erda-badge-status-dot" style={color ? { backgroundColor: color } : {}}>
            <span className="erda-badge-status-breath" />
          </span>
        ) : null}
        <span className="erda-badge-status-text">{text}</span>
      </span>
    </Tooltip>
  );
};

Badge.BadgeStatus = BadgeStatus;

export default Badge;
