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

export const ciStatusMap = {
  Initializing: {
    text: i18n.t('initialize'),
    color: 'gray',
    status: 'default',
  },
  Analyzed: {
    text: i18n.t('analysis completed'),
    color: 'green',
    status: 'success',
  },
  AnalyzeFailed: {
    text: i18n.t('analysis failed'),
    color: 'red',
    status: 'error',
  },
  Born: {
    text: i18n.t('initialize'),
    color: 'gray',
    status: 'default',
  },
  Mark: {
    text: i18n.t('ready to execute'),
    color: 'blue',
    status: 'default',
  },
  Created: {
    text: i18n.t('establish'),
    color: 'gray',
    status: 'default',
  },
  Queue: {
    text: i18n.t('queuing'),
    icon: 'puse-circle',
    color: 'blue',
    status: 'processing',
  },
  Running: {
    text: i18n.t('running'),
    icon: 'puse-circle',
    color: 'blue',
    status: 'processing',
  },
  LostConn: {
    text: i18n.t('lost connection'),
    color: 'red',
    status: 'error',
  },
  Unknown: {
    text: i18n.t('unknown'),
    color: 'gray',
    status: 'warning',
  },

  Success: {
    text: i18n.t('succeed'),
    icon: 'play1',
    color: 'green',
    status: 'success',
  },
  Failed: {
    text: i18n.t('failed'),
    color: 'red',
    status: 'warning',
  },
  Error: {
    text: i18n.t('error'),
    color: 'red',
    status: 'error',
  },
  Timeout: {
    text: i18n.t('timeout'),
    color: 'red',
    status: 'error',
  },
  CancelByRemote: {
    text: i18n.t('stop by system'),
    color: 'gray',
    status: 'warning',
  },
  StopByUser: {
    text: i18n.t('stop by user'),
    color: 'orange',
    status: 'warning',
  },
  NoNeedBySystem: {
    text: i18n.t('no need to execute'),
    color: 'gray',
    status: 'default',
  },
  CreateError: {
    text: i18n.t('establish error'),
    color: 'red',
    status: 'error',
  },
  StartError: {
    text: i18n.t('startup error'),
    color: 'red',
    status: 'error',
  },
  DBError: {
    text: i18n.t('DB error'),
    color: 'red',
    status: 'error',
  },
  StopCron: {
    text: i18n.t('stop cron'),
    color: 'orange',
    status: 'warning',
  },
  WaitCron: {
    text: i18n.t('pending execution'),
    color: 'blue',
    status: 'default',
  },
  Paused: {
    text: i18n.t('pause'),
    color: 'orange',
    status: 'warning',
  },
  '': {
    text: '',
    color: 'gray',
    status: 'default',
  },
  Disabled: {
    text: i18n.t('disabled'),
    color: 'gray',
    status: 'default',
  },
  WaitApprove: {
    text: i18n.t('pending approval'),
    color: 'blue',
    status: 'default',
  },
  Accept: {
    text: i18n.t('approval passed'),
    color: 'green',
    status: 'success',
  },
  Reject: {
    text: i18n.t('approval failed'),
    color: 'red',
    status: 'error',
  },
};

export const serviceStatusMap = {
  Progressing: {
    text: i18n.t('deploying'),
    color: 'gray',
  },
  UnHealthy: {
    text: i18n.t('unhealthy'),
    color: 'red',
  },
  Healthy: {
    text: i18n.t('healthy'),
    color: 'green',
  },
};

export const ciBuildStatusSet = {
  executeStatus: ['Born', 'Mark', 'Queue', 'Running', 'Created'],
  beforeRunningStatus: ['Initializing', 'Analyzed', 'Born'],
};

export const ciNodeStatusSet = {
  loadingStatus: ['Queue', 'Running'],
  executeStatus: [
    'Running',
    'Success',
    'Failed',
    'Error',
    'LostConn',
    'Unknown',
    'Timeout',
    'CancelByRemote',
    'StopByUser',
    'StartError',
    'DBError',
  ],
};
