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

export const chartConfig = {
  memory: {
    unitType: 'CAPACITY',
    titleText: i18n.t('memory'),
    name: 'mem',
    xAxis: {
      type: 'line',
    },
  },
  cpu: {
    unitType: 'PERCENT',
    titleText: 'CPU',
    decimal: 2,
    name: 'cpu',
    xAxis: {
      type: 'line',
    },
  },
  disk: {
    unitType: 'TRAFFIC',
    rightUnitType: 'TRAFFIC',
    titleText: i18n.t('disk'),
    isTwoYAxis: true,
    decimal: 2,
    name: 'disk',
    xAxis: {
      type: 'line',
    },
    // noAreaColor: 'true',
  },
};

export const titleCnMap = {
  PROJECT: i18n.t('project'),
  APPLICATION: i18n.t('application'),
  RUNTIME: 'RUNTIME',
  SERVICE: i18n.t('service'),
  CONTAINER: 'CONTAINER',
};

export const titleMap = ['PROJECT', 'APPLICATION', 'RUNTIME', 'SERVICE'];
export const iconMap = ['project', 'wenjianjia', 'fengchao', 'atom'];
