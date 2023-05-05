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

export const getServiceInsList = (insId: string): JVM.ServiceIns[] => {
  return agent.get(`/api/monitor/jvmprofiler/${insId}/services`).then((response: any) => response.body);
};

export const getProfileList = ({ insId, state, ...rest }: JVM.ProfileListQuery): IPagingResp<JVM.ProfileItem> => {
  return agent
    .get(`/api/monitor/jvmprofiler/${insId}/profiling`)
    .query({ state, ...rest })
    .then((response: any) => response.body);
};

export const startProfile = ({ insId, ...body }: JVM.StartProfileBody): JVM.PendingProfile => {
  return agent
    .post(`/api/monitor/jvmprofiler/${insId}/profiling`)
    .send(body)
    .then((response: any) => response.body);
};

export const stopProfile = ({ insId, profileId }: JVM.ProfileStatusQuery): JVM.PendingProfile => {
  return agent
    .put(`/api/monitor/jvmprofiler/${insId}/profiling/${profileId}/actions/finish`)
    .then((response: any) => response.body);
};

export const getProfileStatus = ({ insId, profileId }: JVM.ProfileStatusQuery): JVM.PendingProfile => {
  return agent
    .get(`/api/monitor/jvmprofiler/${insId}/profiling/${profileId}/state`)
    .then((response: any) => response.body);
};

export const getJVMInfo = ({ insId, profileId, scope }: JVM.JVMInfoQuery) => {
  return agent
    .get(`/api/monitor/jvmprofiler/${insId}/profiling/${profileId}/jvm-info`)
    .query({ scope })
    .then((response: any) => response.body);
};
