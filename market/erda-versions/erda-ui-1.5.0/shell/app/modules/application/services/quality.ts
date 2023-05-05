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

// api: https://yuque.antfin-inc.com/terminus_paas_dev/devops/dnne8h#dbziog
// 后端: @溪杨
export const getLatestSonarStatistics = ({ applicationId }: { applicationId: string }): QA.Stat => {
  return agent
    .get('/api/qa')
    .query({ applicationId })
    .then((response: any) => response.body);
};

export const getSonarResults = ({ type, key }: { type: string; key: string }): QA.Item[] => {
  return agent
    .get('/api/qa')
    .query({ type, key })
    .then((response: any) => response.body);
};
