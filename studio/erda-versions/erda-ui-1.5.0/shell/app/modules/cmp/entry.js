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

import routers from './router';
import { createCustomAlarmStore } from './stores/_common-custom-alarm';
import { createCustomDashboardStore } from './stores/_common-custom-dashboard';
import alarmRecordStore from './stores/alarm-record';
import alarmReportStore from './stores/alarm-report';
import alarmStrategyStore from './stores/alarm-strategy';
import monitorMetadataStore from './stores/analysis-monitor-metadata';
import cloudAccountStore from './stores/cloud-account';
import cloudCommonStore from './stores/cloud-common';
import cloudSourceStore from './stores/cloud-source';
import clusterStore from './stores/cluster';
import computingStore from './stores/computing';
import customAlarmStore from './stores/custom-alarm';
import customDashboardStore from './stores/custom-dashboard';
import machineStore from './stores/machine';
import middlewareChartStore from './stores/middleware-chart';
import middlewareDashboardStore from './stores/middleware-dashboard';
import networksStore from './stores/networks';
import podDetailStore from './stores/pod-detail';
import queryMonitorStore from './stores/query-monitor-metadata';
import storageStore from './stores/storage';
import taskStore from './stores/task';

export default (registerModule) => {
  return registerModule({
    key: 'cmp',
    stores: [
      customAlarmStore,
      customDashboardStore,
      alarmRecordStore,
      alarmReportStore,
      alarmStrategyStore,
      monitorMetadataStore,
      cloudAccountStore,
      cloudCommonStore,
      cloudSourceStore,
      createCustomAlarmStore,
      clusterStore,
      computingStore,
      createCustomDashboardStore,
      machineStore,
      middlewareChartStore,
      middlewareDashboardStore,
      networksStore,
      podDetailStore,
      queryMonitorStore,
      storageStore,
      taskStore,
    ],
    routers,
  });
};
