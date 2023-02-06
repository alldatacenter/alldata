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

import React, { useState, useEffect, useMemo } from 'react';
import { BoardGrid } from 'common';
import monitorCommonStore from 'common/stores/monitorCommon';
import dashboardStore from 'common/stores/dashboard';
import routeInfoStore from 'core/stores/route';
import { isEqual } from 'lodash';
import serviceAnalyticsStore from 'msp/stores/service-analytics';
import mspStore from 'msp/stores/micro-service';

type IProps = Merge<
  Partial<DC.PureBoardGridProps>,
  {
    dashboardId: string;
    extraGlobalVariable?: Record<string, any>;
    timeSpan?: ITimeSpan;
  }
>;

const ServiceListDashboard: React.FC<IProps> = ({ timeSpan: times, dashboardId, extraGlobalVariable, ...rest }) => {
  const { range } = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan);
  // when the parent component depends on timeSpan, use the timeSpan of the parent component to prevent duplicate requests
  const timeSpan = times || range;
  const params = routeInfoStore.useStore((s) => s.params);
  const { getCustomDashboard } = dashboardStore;
  const [layout, setLayout] = useState<DC.Layout>([]);
  const [serviceId, serviceName] = serviceAnalyticsStore.useStore((s) => [s.serviceId, s.serviceName]);
  const type = mspStore.useStore((s) => s.currentProject.type);

  const globalVariable = useMemo(() => {
    const { terminusKey } = params;
    const { startTimeMs, endTimeMs } = timeSpan;
    return {
      terminusKey,
      serviceName,
      serviceId: window.decodeURIComponent(serviceId),
      startTime: startTimeMs,
      endTime: endTimeMs,
      ...extraGlobalVariable,
    };
  }, [params, timeSpan, serviceName, serviceId, extraGlobalVariable]);

  useEffect(() => {
    getCustomDashboard({ id: dashboardId, isSystem: true }).then((res) => {
      setLayout(res);
    });
  }, [dashboardId, getCustomDashboard]);

  return <BoardGrid.Pure globalVariable={globalVariable} layout={layout} {...rest} />;
};

export default React.memo(ServiceListDashboard, (prev, next) => {
  return (
    isEqual(prev.extraGlobalVariable, next.extraGlobalVariable) &&
    prev.dashboardId === next.dashboardId &&
    isEqual(prev.timeSpan, next.timeSpan)
  );
});
