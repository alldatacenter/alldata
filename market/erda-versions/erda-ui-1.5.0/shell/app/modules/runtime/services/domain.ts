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

export const getDomains = ({ runtimeId, domainType }: RUNTIME_DOMAIN.Query): RUNTIME_DOMAIN.DomainMap => {
  return agent.get(`/api/runtimes/${runtimeId}/${domainType}`).then((response: any) => response.body);
};

export const updateDomains = ({ runtimeId, data }: { runtimeId: string; data: RUNTIME_DOMAIN.UpdateK8SDomainBody }) => {
  return agent
    .put(`/api/runtimes/${runtimeId}/domains`)
    .send(data)
    .then((response: any) => response.body);
};

export const updateK8SDomain = ({ runtimeId, serviceName, domains, releaseId }: RUNTIME_DOMAIN.UpdateK8SDomainBody) => {
  return agent
    .put(`/api/runtimes/${runtimeId}/services/${serviceName}/domains`)
    .send({ releaseId, domains })
    .then((response: any) => response.body);
};
