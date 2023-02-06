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

export const getBlackList = ({ artifactId, ...query }: PUBLISHER.IListQuery): IPagingResp<PUBLISHER.IBlackList> => {
  return agent
    .get(`/api/publish-items/${artifactId}/blacklist`)
    .query(query)
    .then((response: any) => response.body);
};

export const addBlackList = ({ artifactId, ...data }: { artifactId: string }) => {
  return agent
    .post(`/api/publish-items/${artifactId}/blacklist`)
    .send(data)
    .then((response: any) => response.body);
};

export const deleteBlackList = ({ artifactId, blacklistId }: { artifactId: string; blacklistId: string }) => {
  return agent
    .delete(`/api/publish-items/${artifactId}/blacklist/${blacklistId}`)
    .then((response: any) => response.body);
};

export const getEraseList = ({ artifactId, ...query }: PUBLISHER.IListQuery): IPagingResp<PUBLISHER.IErase> => {
  return agent
    .get(`/api/publish-items/${artifactId}/erase`)
    .query(query)
    .then((response: any) => response.body);
};

export const addErase = ({ artifactId, ...data }: { artifactId: string }) => {
  return agent
    .post(`/api/publish-items/${artifactId}/erase`)
    .send(data)
    .then((response: any) => response.body);
};
