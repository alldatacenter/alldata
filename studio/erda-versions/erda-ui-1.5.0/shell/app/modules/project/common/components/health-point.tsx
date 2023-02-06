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
import { Badge, Tooltip } from 'antd';
import i18n from 'i18n';

import './health-point.scss';

export const statusMap = {
  runtime: {
    Init: {
      text: i18n.t('initializing'),
      state: 'processing',
    },
    Progressing: {
      text: i18n.t('deploying'),
      state: 'processing',
    },
    DEPLOYING: {
      text: i18n.t('deploying'),
      state: 'processing',
    },
    UnHealthy: {
      text: i18n.t('unhealthy'),
      state: 'warning',
    },
    OK: {
      text: i18n.t('dop:health'),
      state: 'success',
    },
    Healthy: {
      text: i18n.t('dop:health'),
      state: 'success',
    },
  },
  service: {
    Progressing: {
      text: i18n.t('deploying'),
      state: 'processing',
    },
    UnHealthy: {
      text: i18n.t('unhealthy'),
      state: 'warning',
    },
    Healthy: {
      text: i18n.t('dop:health'),
      state: 'success',
    },
  },
  task: {
    Killed: {
      text: i18n.t('stopped'),
      state: 'error',
    },
    Stopped: {
      text: i18n.t('stopped'),
      state: 'error',
    },
    Failed: {
      text: i18n.t('dop:deployment failed'),
      state: 'error',
    },
    Finished: {
      text: i18n.t('dop:complete'),
      state: 'default',
    },
    Starting: {
      text: i18n.t('dop:starting'),
      state: 'processing',
    },
    Healthy: {
      text: i18n.t('dop:health'),
      state: 'success',
    },
    UnHealthy: {
      text: i18n.t('unhealthy'),
      state: 'warning',
    },
    Unknown: {
      text: i18n.t('unknown'),
      state: 'warning',
    },
    OOM: {
      text: i18n.t('dop:oom'),
      state: 'warning',
    },
    Running: {
      text: i18n.t('running'),
      state: 'success',
    },
    Dead: {
      text: i18n.t('stopped'),
      state: 'error',
    },
  },
  machine: {
    fatal: {
      text: i18n.t('error'),
      state: 'error',
    },
    warning: {
      text: i18n.t('unhealthy'),
      state: 'warning',
    },
    normal: {
      text: i18n.t('dop:health'),
      state: 'success',
    },
  },
};

const fallback = { text: i18n.t('unknown'), state: 'default' };

interface IProps {
  type: string;
  status: string;
  msg?: string;
  subText?: React.ElementType | null;
  showText?: boolean;
  showTextLeft?: boolean;
}
const HealthPoint = ({ type, msg = '', status, subText = null, showText = false, showTextLeft = false }: IProps) => {
  const match = (statusMap[type] && statusMap[type][status]) || fallback;
  const { state } = match;
  const text = msg || match.text;

  if (showText) {
    return (
      <span className="health-point">
        <Badge status={state} />{' '}
        <span className="health-text">
          {text}
          {subText}
        </span>
      </span>
    );
  }

  if (showTextLeft) {
    return (
      <span className="health-point">
        <span className="health-text">{text} ·</span>
        <Badge status={state} />
      </span>
    );
  }

  return (
    <Tooltip title={text}>
      <span className="health-point">
        <Badge status={state} />
      </span>
    </Tooltip>
  );
};

export default HealthPoint;
