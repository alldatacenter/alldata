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

import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import React from 'react';
import { useEffectOnce } from 'react-use';
import { useInstanceOperation } from 'cmp/common/components/instance-operation';
import middlewareDashboardStore from 'cmp/stores/middleware-dashboard';
import { PureResourceList } from 'cmp/common/addon-detail/resource';

const Resource = () => {
  const resourceList = middlewareDashboardStore.useStore((s) => s.resourceList);
  const { params } = routeInfoStore.getState((s) => s);
  const [loading] = useLoading(routeInfoStore, ['getResourceList']);
  const [renderOp, drawerComp] = useInstanceOperation<MIDDLEWARE_DASHBOARD.IResource>({
    log: true,
    monitor: true,
    getProps(type, record) {
      return {
        log: {
          hasLogs: false,
          fetchApi: `/api/addons/${record.instanceId}/logs`,
        },
        monitor: {
          api: '/api/orgCenter/metrics',
        },
      }[type];
    },
  });

  useEffectOnce(() => {
    middlewareDashboardStore.effects.getResourceList(params.instanceId);
    return () => middlewareDashboardStore.reducers.clearResourceList();
  });

  return <PureResourceList renderOp={renderOp} resourceList={resourceList} loading={loading} drawerComp={drawerComp} />;
};

export default Resource;
