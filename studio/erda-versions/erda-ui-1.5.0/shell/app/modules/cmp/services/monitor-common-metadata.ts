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

export const getMetaGroups = (q: MONITOR_COMMON_METADATA.CommonMetaQuery): any[] => {
  return agent
    .get('/api/metadata/groups')
    .query(q)
    .then((response: any) => response.body);
};

export const getMetaData = ({
  groupId,
  ...q
}: Merge<{ groupId: string }, MONITOR_COMMON_METADATA.CommonMetaQuery>): MONITOR_COMMON_METADATA.MetaData => {
  return agent
    .get(`/api/metadata/groups/${groupId}`)
    .query(q)
    .then((response: any) => response.body);
};
