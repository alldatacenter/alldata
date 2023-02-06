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

import agent from 'agent';
import { RES_BODY } from 'core/service';

export const getOrgByDomain = (payload: ORG.IOrgReq): ORG.IOrg => {
  return agent
    .get('/api/orgs/actions/get-by-domain')
    .query(payload)
    .then((response: any) => response.body);
};

export const getJoinedOrgs = (): Promise<RES_BODY<IPagingResp<ORG.IOrg>>> => {
  return agent
    .get('/api/orgs')
    .query({ pageNo: 1, pageSize: 100 })
    .then((response: any) => response.body);
};

export const updateOrg = (org: Merge<Partial<ORG.IOrg>, { id: number }>) => {
  return agent
    .put(`/api/orgs/${org.id}`)
    .send(org)
    .then((response: any) => response.body);
};
