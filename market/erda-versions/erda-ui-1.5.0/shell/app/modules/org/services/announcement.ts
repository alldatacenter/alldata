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

export const getAnnouncementList = (params: ORG_ANNOUNCEMENT.QueryList): IPagingResp<ORG_ANNOUNCEMENT.Item> => {
  return agent
    .get('/api/notices')
    .query(params)
    .then((response: any) => response.body);
};

export const addAnnouncement = (params: ORG_ANNOUNCEMENT.SaveNew) => {
  return agent
    .post('/api/notices')
    .send(params)
    .then((response: any) => response.body);
};

export const updateAnnouncement = (params: ORG_ANNOUNCEMENT.SaveEdit) => {
  const { id, ...rest } = params;
  return agent
    .put(`/api/notices/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const publishAnnouncement = ({ id }: ORG_ANNOUNCEMENT.Action) => {
  return agent.put(`/api/notices/${id}/actions/publish`).then((response: any) => response.body);
};

export const unPublishAnnouncement = ({ id }: ORG_ANNOUNCEMENT.Action) => {
  return agent.put(`/api/notices/${id}/actions/unpublish`).then((response: any) => response.body);
};

export const deleteAnnouncement = ({ id }: ORG_ANNOUNCEMENT.Action) => {
  return agent.delete(`/api/notices/${id}`).then((response: any) => response.body);
};
