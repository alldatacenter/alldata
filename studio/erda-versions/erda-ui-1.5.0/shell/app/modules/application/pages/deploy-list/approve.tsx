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
import DeployList from './deploy-list';

enum MY_APPROVAL_STATE {
  pending = 'pending',
  approved = 'approved',
}

const Approve = () => {
  const status = routeInfoStore.getState((s) => s.params.approvalType) || MY_APPROVAL_STATE.pending;
  const [approvalList, approvalPaging] = deployStore.useStore((s) => [s.approvalList, s.approvalPaging]);
  const { getApprovalList, updateApproval } = deployStore.effects;
  const { clearDeployList } = deployStore.reducers;
  const [loading] = useLoading(deployStore, ['getApprovalList']);
  const getList = (approvalStatus: string) => (query: any) => getApprovalList({ ...query, approvalStatus });

  const propsMap = {
    getList: getList(status),
    clearList: () => clearDeployList('approval'),
    list: approvalList,
    paging: approvalPaging,
    isFetching: loading,
    updateApproval,
  };

  return <DeployList key={status} type="approve" status={status} {...propsMap} />;
};

export default Approve;
