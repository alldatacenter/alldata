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

export const TASKS_STATUS_MAP = {
  Initializing: { name: i18n.t('initializing'), state: 'processing' },
  Disabled: { name: i18n.t('disable'), state: 'warning' },
  WaitCron: { name: i18n.t('timing has started, waiting for timing execution'), state: 'processing' },
  StopCron: { name: i18n.t('timing has stopped'), state: 'warning' },
  AnalyzeFailed: { name: i18n.t('analysis failed'), state: 'error' },
  Analyzed: { name: i18n.t('analysis completed'), state: 'success' },
  Born: { name: i18n.t('process initialization'), state: 'processing' },
  Paused: { name: i18n.t('time out'), state: 'warning' },
  Mark: { name: i18n.t('mark'), state: 'processing' },
  Created: { name: i18n.t('created successfully'), state: 'success' },
  Queue: { name: i18n.t('queuing'), state: 'processing' },
  Running: { name: i18n.t('running'), state: 'processing' },
  StatusSuccess: { name: i18n.t('succeed'), state: 'success' },
  Success: { name: i18n.t('succeed'), state: 'success' },
  CreateError: { name: i18n.t('failed to create node'), state: 'error' },
  StartError: { name: i18n.t('failed to start node'), state: 'error' },
  Error: { name: i18n.t('abnormal'), state: 'error' },
  Failed: { name: i18n.t('failed'), state: 'error' },
  Timeout: { name: i18n.t('time out'), state: 'error' },
  DBError: { name: i18n.t('database exception'), state: 'error' },
  Unknown: { name: i18n.t('unknown state'), state: 'warning' },
  LostConn: { name: i18n.t('unable to connect'), state: 'error' },
  CancelByRemote: { name: i18n.t('remote cancellation'), state: 'warning' },
  StopByUser: { name: i18n.t('cancelled by user'), state: 'warning' },
  NoNeedBySystem: { name: i18n.t('no need to execute'), state: 'warning' },
};

export const WORKSPACE_MAP = {
  DEV: i18n.t('dev'),
  TEST: i18n.t('test'),
  STAGING: i18n.t('staging'),
  PROD: i18n.t('prod'),
};
