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
import { MetricsMonitor, Holder } from 'common';
import routeInfoStore from 'core/stores/route';
import middlewareDashboardStore from '../../stores/middleware-dashboard';

const Monitor = () => {
  const baseInfo = middlewareDashboardStore.useStore((s) => s.baseInfo);
  const { getBaseInfo } = middlewareDashboardStore.effects;
  const { params } = routeInfoStore.getState((s) => s);

  React.useEffect(() => {
    if (params.instanceId) {
      getBaseInfo(params.instanceId);
    }
  }, [getBaseInfo, params.instanceId]);

  return (
    <Holder when={!baseInfo.addonName || !baseInfo.cluster || !baseInfo.instanceId}>
      <MetricsMonitor
        resourceType={baseInfo.addonName}
        resourceId={baseInfo.instanceId}
        commonChartQuery={{
          filter_cluster_name: baseInfo.cluster,
          filter_addon_id: baseInfo.instanceId,
          customAPIPrefix: '/api/addon/metrics/charts/',
        }}
      />
    </Holder>
  );
};

export default Monitor;
