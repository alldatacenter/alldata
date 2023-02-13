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
import { Spin } from 'antd';
import { IF, MetricsMonitor } from 'common';
import PureAddonResource from './pure-addon-resource';
import addonStore from 'common/stores/addon';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';

const AddonResource = () => {
  const info = addonStore.useStore((s) => s.addonDetail);
  const insId = routeInfoStore.useStore((s) => s.params.insId);
  const [loading] = useLoading(addonStore, ['getAddonDetail']);
  React.useEffect(() => {
    addonStore.getAddonDetail(insId);
  }, [insId]);
  const { addonName, cluster, realInstanceId } = info as any;
  return (
    <Spin spinning={loading}>
      <PureAddonResource resourceInfo={info} resourceId={insId} />
      <IF check={addonName && cluster && realInstanceId}>
        <MetricsMonitor
          resourceType={addonName}
          resourceId={insId}
          commonChartQuery={{
            filter_cluster_name: cluster,
            filter_addon_id: realInstanceId,
            customAPIPrefix: '/api/addon/metrics/charts/',
          }}
        />
      </IF>
    </Spin>
  );
};

export default AddonResource;
