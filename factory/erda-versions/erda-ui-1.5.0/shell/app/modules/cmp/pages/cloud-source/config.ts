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

import i18n from 'i18n';

export const RUNNING_STATUS_LIST = [i18n.t('running'), i18n.t('cmp:will expire soon')]; // 被判断为正在运行的状态的列表
export const STOP_STATUS_LIST = [i18n.t('stopped'), i18n.t('stop'), i18n.t('cmp:expired')];

const serviceStatusMap = {
  Running: { status: 'success' },
  Stopped: { status: 'default' },
  expired: { status: 'error' },
  'will expire soon': { status: 'warning' },
  Starting: { status: 'default' },
  Stopping: { status: 'default' },
  运行中: { status: 'success' },
  停止: { status: 'default' },
  已过期: { status: 'error' },
  即将过期: { status: 'warning' },
};
const sourceStatusMap = {
  Available: { status: 'success' },
  Unavailable: { status: 'error' },
  可用: { status: 'success' },
  不可用: { status: 'error' },
};
export const statusMap = {
  ecs: serviceStatusMap,
  rds: serviceStatusMap,
  vpc: sourceStatusMap,
  vsw: sourceStatusMap,
  redis: {
    Normal: { status: 'success' },
    Abnormal: { status: 'error' },
    正常: { status: 'success' },
    异常: { status: 'error' },
  },
  mq: serviceStatusMap,
  account: sourceStatusMap,
};

const serviceStatus = ['Running', '运行中'];
export const skipInfoStatusMap = {
  rds: serviceStatus,
  mq: serviceStatus,
  redis: ['Normal', '正常'],
};
