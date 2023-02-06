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

export const getAccessList = (payload: API_ACCESS.QueryAccess): IPagingResp<API_ACCESS.AccessListItem> => {
  return agent
    .get('/api/api-access')
    .query(payload)
    .then((response: any) => response.body);
};

export const createAccess = (payload: API_ACCESS.CreateAccess): Pick<API_ACCESS.AccessDetail, 'access'> => {
  return agent
    .post('/api/api-access')
    .send(payload)
    .then((response: any) => response.body);
};

export const updateAccess = ({ accessID, ...rest }: Merge<API_ACCESS.CreateAccess, { accessID: number }>) => {
  return agent
    .put(`/api/api-access/${accessID}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const getAccessDetail = ({ accessID }: API_ACCESS.AccessID): API_ACCESS.AccessDetail => {
  return agent.get(`/api/api-access/${accessID}`).then((response: any) => response.body);
};

export const deleteAccess = ({ accessID }: API_ACCESS.AccessID) => {
  return agent.delete(`/api/api-access/${accessID}`).then((response: any) => response.body);
};

export const getClientList = ({ assetID, swaggerVersion }: API_ACCESS.QueryClient): IPagingResp<API_ACCESS.Client> => {
  return agent
    .get(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/clients`)
    .then((response: any) => response.body);
};

export const getClientInfo = ({ clientID, contactID }: API_ACCESS.QueryContractInfo) => {
  return agent.get(`/api/api-clients/${clientID}/contracts/${contactID}/`).then((response: any) => response.body);
};

export const getApiGateway = ({ projectID }: { projectID: number }): API_MARKET.CommonData<API_ACCESS.ApiGateway[]> => {
  return agent.get(`/api/api-gateways/${projectID}`).then((response: any) => response.body);
};

export const getOperationRecord = ({
  clientID,
  contractID,
}: {
  clientID: number;
  contractID: number;
}): API_MARKET.CommonData<API_ACCESS.OperationRecord[]> => {
  return agent
    .get(`/api/api-clients/${clientID}/contracts/${contractID}/operation-records`)
    .then((response: any) => response.body);
};

export const deleteContracts = ({ clientID, contractID }: Omit<API_ACCESS.OperateContract, 'status'>) => {
  return agent.delete(`/api/api-clients/${clientID}/contracts/${contractID}`).then((response: any) => response.body);
};

export const updateContracts = ({ clientID, contractID, ...rest }: API_ACCESS.OperateContract) => {
  return agent
    .put(`/api/api-clients/${clientID}/contracts/${contractID}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const getSlaList = <T = API_ACCESS.SlaData>({ assetID, swaggerVersion }: API_ACCESS.GetSlaList): T => {
  return agent
    .get(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/slas`)
    .then((response: any) => response.body);
};

export const getSla = ({ assetID, swaggerVersion, slaID }: API_ACCESS.GetSla): API_ACCESS.SlaItem => {
  return agent
    .get(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/slas/${slaID}`)
    .then((response: any) => response.body);
};

export const addSla = ({ assetID, swaggerVersion, ...rest }: API_ACCESS.AddSla) => {
  return agent
    .post(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/slas`)
    .send(rest)
    .then((response: any) => response.body);
};

export const updateSla = ({ assetID, swaggerVersion, slaID, ...rest }: API_ACCESS.UpdateSla) => {
  return agent
    .put(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/slas/${slaID}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deleteSla = ({ assetID, swaggerVersion, slaID }: API_ACCESS.DeleteSla) => {
  return agent
    .delete(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/slas/${slaID}`)
    .then((response: any) => response.body);
};
export const getDashboard = ({ type }: API_ACCESS.QueryDashboard) => {
  return agent.get(`/api/dashboard/system/blocks/${type}`).then((response: any) => response.body);
};
