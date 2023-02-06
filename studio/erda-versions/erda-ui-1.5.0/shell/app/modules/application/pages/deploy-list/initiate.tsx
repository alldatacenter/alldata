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
import routeInfoStore from 'core/stores/route';
import deployStore from 'application/stores/deploy';
import { useLoading } from 'core/stores/loading';
import DeployList, { approvalStatusMap } from './deploy-list';
import runtimeStore from 'runtime/stores/runtime';

const Initiate = () => {
  const status = routeInfoStore.getState((s) => s.params.initiateType) || approvalStatusMap.WaitApprove.value;
  const [launchedDeployList, launchedDeployPaging] = deployStore.useStore((s) => [
    s.launchedDeployList,
    s.launchedDeployPaging,
  ]);
  const { getLaunchedDeployList } = deployStore.effects;
  const { clearDeployList } = deployStore.reducers;
  const [loading] = useLoading(deployStore, ['getLaunchedDeployList']);
  const { cancelDeployment } = runtimeStore;

  const getList = (approvalStatus: string) => (query: any) => getLaunchedDeployList({ ...query, approvalStatus });

  const propsMap = {
    getList: getList(status),
    clearList: () => clearDeployList('launched'),
    list: launchedDeployList,
    paging: launchedDeployPaging,
    isFetching: loading,
    cancelDeployment,
  };
  return <DeployList key={status} type="initiate" status={status} {...propsMap} />;
};

export default Initiate;
