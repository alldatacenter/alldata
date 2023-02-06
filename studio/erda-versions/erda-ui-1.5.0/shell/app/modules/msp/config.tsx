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
import i18n from 'i18n';
import { goTo } from 'common/utils';
import { ErdaIcon } from 'common';

export const envMap = {
  DEV: i18n.t('common:DEV'),
  TEST: i18n.t('common:TEST'),
  STAGING: i18n.t('common:STAGING'),
  PROD: i18n.t('common:PROD'),
  DEFAULT: i18n.t('common:DEFAULT'),
};

interface IMSPathParams {
  projectId: string | number;
  env: string;
  tenantGroup: string;
  tenantId: string | number;
  terminusKey: string;
  logKey: string;
}

export const getMSFrontPathByKey = (key: MS_INDEX.IMenuKey, params: IMSPathParams) => {
  const { projectId, env, tenantGroup, tenantId, terminusKey, logKey } = params;

  const rootPath = `${goTo.resolve.mspOverviewRoot({ projectId, env, tenantGroup })}/`;
  // service monitor
  const monitorPrefix = `monitor/${terminusKey}`;
  // alarm management
  const alarmManagementPrefix = `alarm-management/${terminusKey}`;
  // env synopsis
  const envOverViewPrefix = `synopsis/${terminusKey}`;

  const diagnoseAnalyzerPrefix = `${terminusKey}`;

  const serviceManagePrefix = 'service-manage';

  const targetPath: { [key in MS_INDEX.IMenuKey]: string } = {
    Overview: `${envOverViewPrefix}/service-list`,
    MonitorCenter: monitorPrefix,
    ServiceMonitor: `${monitorPrefix}/service-analysis`,
    FrontMonitor: `${monitorPrefix}/bi`,
    ActiveMonitor: `${monitorPrefix}/status`,
    AlertCenter: alarmManagementPrefix,
    AlertStrategy: `${alarmManagementPrefix}/alarm`,
    AlarmHistory: `${alarmManagementPrefix}/alarm-record`,
    RuleManagement: `${alarmManagementPrefix}/custom-alarm`,
    NotifyGroupManagement: `${alarmManagementPrefix}/notify-group`,
    DiagnoseAnalyzer: diagnoseAnalyzerPrefix,
    Tracing: `${monitorPrefix}/trace`,
    LogAnalyze: `log/${logKey}`,
    ErrorInsight: `${monitorPrefix}/error`,
    Dashboard: `analysis/${terminusKey}/custom-dashboard`,
    ServiceManage: serviceManagePrefix,
    APIGateway: 'gateway',
    RegisterCenter: 'services',
    ConfigCenter: `config/${tenantId}`,
    EnvironmentSet: 'environment',
    AccessConfig: `environment/${terminusKey}/configuration`,
    MemberManagement: `environment/${terminusKey}/member`,
    ComponentInfo: `environment/info${tenantId ? `/${tenantId}` : ''}`,
  };
  // FIXME some old addon's key is not in the MSP menu, compatible with MSP history addon jump logic
  const backupPath = {
    Configs: targetPath.ConfigCenter,
    Services: targetPath.RegisterCenter,
    APIs: targetPath.APIGateway,
  };

  return `${rootPath}${targetPath[key] ?? backupPath[key] ?? ''}`.replace(/\/$/, '');
};

export const getMSPSubtitleByName = (key: MS_INDEX.IRootMenu) => {
  const MSPSubtitleMap: { [key in MS_INDEX.IRootMenu]: { zh: string; en: string } } = {
    Overview: {
      zh: '拓扑',
      en: 'Topology',
    },
    MonitorCenter: {
      zh: '监控',
      en: 'Monitor',
    },
    AlertCenter: {
      zh: '告警',
      en: 'Alarm',
    },
    DiagnoseAnalyzer: {
      zh: '诊断',
      en: 'Diagnose',
    },
    ServiceManage: {
      zh: '治理',
      en: 'Manage',
    },
    EnvironmentSet: {
      zh: '设置',
      en: 'Set',
    },
  };

  return MSPSubtitleMap[key];
};

const renderIcon = (type: string) => () => {
  return <ErdaIcon className="erda-icon" type={type} fill="primary" />;
};

export const MSIconMap = {
  Overview: renderIcon('huanjinggailan'),
  ServiceManage: renderIcon('zhili'),
  AlertCenter: renderIcon('gaojingguanli'),
  EnvironmentSet: renderIcon('components'),
  MonitorCenter: renderIcon('monitor-camera'),
  DiagnoseAnalyzer: renderIcon('log'),
};
