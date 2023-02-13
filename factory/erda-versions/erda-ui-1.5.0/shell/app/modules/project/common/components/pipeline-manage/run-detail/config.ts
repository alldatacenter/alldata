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
};

export const ciBuildStatusSet = {
  executeStatus: ['Born', 'Mark', 'Queue', 'Running', 'Created'],
  beforeRunningStatus: ['Initializing', 'Analyzed', 'Born'],
  runningStatus: ['Running', 'Analyzed'],
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
    'CreateError',
  ],
};

export const PipelineStatus = [
  { status: 'Initializing', msg: i18n.t('initializing'), colorClass: 'gray' },
  { status: 'Analyzed', msg: i18n.t('analysis completed'), colorClass: 'darkgray' },
  { status: 'AnalyzeFailed', msg: i18n.t('analysis failed'), colorClass: 'red' },
  { status: 'WaitCron', msg: i18n.t('cron has started, waiting for execution'), colorClass: 'blue' },
  { status: 'StopCron', msg: i18n.t('cron has stopped'), colorClass: 'yellow' },
  { status: 'Born', msg: i18n.t('pending execution'), colorClass: 'darkgray', jumping: true },
  { status: 'Mark', msg: i18n.t('pending execution'), colorClass: 'darkgray', jumping: true },
  { status: 'Created', msg: i18n.t('established successfully'), colorClass: 'darkgray', jumping: true },
  { status: 'Queue', msg: i18n.t('queuing for resources'), colorClass: 'blue' },
  { status: 'Running', msg: i18n.t('running'), colorClass: 'blue', jumping: true },
  // { status: 'Success' 成功 },
  { status: 'Success', msg: i18n.t('succeed'), colorClass: 'green' },
  { status: 'Failed', msg: i18n.t('failed'), colorClass: 'red' },
  { status: 'Timeout', msg: i18n.t('timeout'), colorClass: 'red' },
  { status: 'StopByUser', msg: i18n.t('stop by user'), colorClass: 'red' },
  { status: 'NoNeedBySystem', msg: i18n.t('no need to execute'), colorClass: 'red' },
  { status: 'CreateError', msg: i18n.t('establish error'), colorClass: 'red' },
  { status: 'StartError', msg: i18n.t('startup error'), colorClass: 'red' },
  { status: 'Error', msg: i18n.t('error'), colorClass: 'red' },
  { status: 'DBError', msg: i18n.t('DB error'), colorClass: 'red' },
  { status: 'Unknown', msg: i18n.t('unknown'), colorClass: 'red' },
  { status: 'LostConn', msg: i18n.t('still unable to connect after trying again'), colorClass: 'red' },
  { status: 'CancelByRemote', msg: i18n.t('stop by system'), colorClass: 'red' },
];
