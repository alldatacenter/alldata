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

export const getServiceInstances = (query: RUNTIME_SERVICE.ListQuery): RUNTIME_SERVICE.Instance[] => {
  return agent
    .get('/api/instances/actions/get-service')
    .query(query)
    .then((response: any) => response.body);
};

export const updateServicesConfig = ({ data, query }: RUNTIME_SERVICE.UpdateConfigBody) => {
  return agent
    .put('/api/runtimes/actions/update-pre-overlay')
    .query(query)
    .send(data)
    .then((response: any) => response.body);
};

export const getServicePods = (query: RUNTIME_SERVICE.PodQuery): RUNTIME_SERVICE.Pod[] => {
  return agent
    .get('/api/instances/actions/get-service-pods')
    .query(query)
    .then((response: any) => response.body);
};

export const killServicePod = (data: RUNTIME_SERVICE.KillPodBody) => {
  return agent
    .post('/api/runtimes/actions/killpod')
    .send(data)
    .then((response: any) => response.body);
};
