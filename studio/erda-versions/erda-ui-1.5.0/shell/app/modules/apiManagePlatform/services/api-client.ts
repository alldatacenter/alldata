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

export const createClient = (payload: API_CLIENT.CreateClient): { client: API_CLIENT.Client } => {
  return agent
    .post('/api/api-clients')
    .send(payload)
    .then((response: any) => response.body);
};

export const getClientList = <T = IPagingResp<API_CLIENT.ClientItem>>(payload: API_CLIENT.QueryClient): T => {
  return agent
    .get('/api/api-clients')
    .query(payload)
    .then((response: any) => response.body);
};

export const getClientDetail = ({ clientID }: API_CLIENT.Common): API_CLIENT.ClientItem => {
  return agent.get(`/api/api-clients/${clientID}`).then((response: any) => response.body);
};

export const deleteClient = ({ clientID }: API_CLIENT.Common) => {
  return agent.delete(`/api/api-clients/${clientID}`).then((response: any) => response.body);
};

export const updateClient = ({
  clientID,
  resetClientSecret,
  ...payload
}: API_CLIENT.UpdateClient): API_CLIENT.ClientItem => {
  return agent
    .put(`/api/api-clients/${clientID}`)
    .query({ resetClientSecret })
    .send(payload)
    .then((response: any) => response.body);
};

export const createContract = ({
  clientID,
  ...rest
}: API_CLIENT.CreteContract): Merge<API_CLIENT.ClientItem, API_CLIENT.ContractItem> => {
  return agent
    .post(`/api/api-clients/${clientID}/contracts`)
    .send(rest)
    .then((response: any) => response.body);
};

export const getContractList = ({
  clientID,
  ...rest
}: API_CLIENT.QueryContractList): IPagingResp<API_CLIENT.ContractItem> => {
  return agent
    .get(`/api/api-clients/${clientID}/contracts`)
    .query({ ...rest })
    .then((response: any) => response.body);
};

export const getContractDetail = ({ clientID, contractID }: API_CLIENT.QueryContract): API_CLIENT.ContractItem => {
  return agent.get(`/api/api-clients/${clientID}/contracts/${contractID}`).then((response: any) => response.body);
};
