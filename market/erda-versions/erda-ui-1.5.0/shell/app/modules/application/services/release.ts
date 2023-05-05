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

export const getReleaseList = (params: RELEASE.ListQuery): IPagingResp<RELEASE.detail> => {
  return agent
    .get('/api/releases')
    .query(params)
    .then((response: any) => response.body);
};

export const updateInfo = ({ releaseId, ...data }: RELEASE.UpdateBody) => {
  return agent
    .put(`/api/releases/${releaseId}`)
    .send(data)
    .then((response: any) => response.body);
};

export const getReleaseDetail = (releaseId: string): RELEASE.detail => {
  return agent.get(`/api/releases/${releaseId}`).then((response: any) => response.body);
};

export const getDiceYml = (releaseId: string): string => {
  const req = agent.get(`/api/releases/${releaseId}/actions/get-dice`);
  req.set('Accept', 'application/x-yaml');
  return (
    req
      // 当前直接返回yml内容，若改为json格式返回这里改成body
      .then((response: any) => response.text)
  );
};
