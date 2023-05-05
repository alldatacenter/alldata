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

interface ICommonQuery {
  projectId: number;
  env: string;
  az: string;
  appId: number;
  nacosId: string;
  tenantGroup: string;
}

export interface IStatusPayload {
  serviceName: string;
  address: string;
  online: boolean;
}

export interface IToggleIPStatus extends ICommonQuery {
  body: IStatusPayload;
}

export /**
 * HTTP 协议页面
 * https://yuque.antfin-inc.com/terminus_paas_dev/vdffob/vd92yn
 * @param {ICommonQuery} { projectId, env, az, appId, nacosId }
 * @returns
 */
const getServiceList = ({ projectId, env, az, appId, nacosId, tenantGroup }: ICommonQuery) => {
  return agent
    .get(`/api/tmc/mesh/listhttpinterface/${projectId}/${env}`)
    .query({ az, appid: appId, nacosId, tenantGroup })
    .then((response: any) => response.body);
};

export const toggleIPStatus = ({ projectId, env, az, appId, nacosId, body, tenantGroup }: IToggleIPStatus) => {
  return agent
    .post(`/api/tmc/mesh/enable/${projectId}/${env}`)
    .query({ az, appid: appId, nacosId, tenantGroup })
    .send(body)
    .then((response: any) => response.body);
};
