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

export const getCloudAccounts = (params: IPagingReq): IPagingResp<CLOUD_ACCOUNTS.Account> => {
  return agent
    .get('/api/cloud-account')
    .query(params)
    .then((response: any) => response.body);
};

export const addCloudAccount = (params: CLOUD_ACCOUNTS.Account) => {
  return agent
    .post('/api/cloud-account')
    .send(params)
    .then((response: any) => response.body);
};

export const deleteCloudAccount = (params: CLOUD_ACCOUNTS.DeleteAccountParam) => {
  return agent
    .delete('/api/cloud-account')
    .send(params)
    .then((response: any) => response.body);
};
