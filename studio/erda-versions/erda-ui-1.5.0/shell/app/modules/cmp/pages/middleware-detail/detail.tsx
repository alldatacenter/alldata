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
import { PureBaseAddonInfo } from 'cmp/common/addon-detail/detail';
import React from 'react';
import { useMount, useUnmount } from 'react-use';
import middlewareDashboardStore from 'cmp/stores/middleware-dashboard';
import ScaleInfo from './scale-info';
import UpgradeInfo from './upgrade-info';
import { getAddonStatus } from 'cmp/services/middleware-dashboard';

const Detail = () => {
  const [addonStatus, setAddonStatus] = React.useState('');
  const [addonDetail] = middlewareDashboardStore.useStore((s) => [s.baseInfo]);
  const { instanceId } = routeInfoStore.getState((s) => s.params);
  const [loading] = useLoading(middlewareDashboardStore, ['getBaseInfo']);
  const { getConfig, getBaseInfo } = middlewareDashboardStore.effects;
  const timer = React.useRef(0 as any);
  useMount(() => {
    getBaseInfo(instanceId).then((res) => {
      const { isOperator, instanceId: addonID, addonName, cluster: clusterName } = res;
      if (isOperator) {
        getConfig({ addonID });
        fetchAddonStatus({ addonID, addonName, clusterName });
        // 轮询状态
        timer.current = setInterval(() => {
          // TODO 2020/7/23 setInterval 和 setTimeout 中调用cube的effect 会导致rerender，待查原因
          fetchAddonStatus({ addonID, addonName, clusterName });
        }, 10000);
      }
    });
    return () => middlewareDashboardStore.reducers.clearBaseInfo();
  });

  const fetchAddonStatus = (data: MIDDLEWARE_DASHBOARD.IMiddleBase) => {
    getAddonStatus(data).then((res: any) => {
      const { status } = res.data;
      if (status !== addonStatus) {
        setAddonStatus(status);
      }
    });
  };

  useUnmount(() => {
    if (timer.current) {
      clearInterval(timer.current);
    }
  });

  const extraNode = React.useMemo(() => {
    let node = null;
    if (addonDetail.isOperator) {
      const data = {
        addonID: instanceId,
        addonName: addonDetail.addonName,
        clusterName: addonDetail.cluster,
        name: addonDetail.name,
        projectID: addonDetail.projectId,
        projectName: addonDetail.projectName,
      };
      node = (
        <>
          <ScaleInfo data={data} />
          <UpgradeInfo data={data} />
        </>
      );
    }
    return node;
  }, [
    addonDetail.addonName,
    addonDetail.cluster,
    addonDetail.isOperator,
    addonDetail.name,
    addonDetail.projectId,
    addonDetail.projectName,
    instanceId,
  ]);

  return <PureBaseAddonInfo addonDetail={{ ...addonDetail, addonStatus }} loading={loading} extra={extraNode} />;
};

export default Detail;
