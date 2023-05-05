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

export const getTaskList = (payload: CLUSTER_TASK.ListQuery): IPagingResp<CLUSTER_TASK.Detail> => {
  return agent
    .get('/api/org/actions/list-running-tasks')
    .query(payload)
    .then((response: any) => {
      const { data, ...rest } = response.body;
      if (data && data.tasks) {
        data.list = data.tasks;
      }
      return { data, ...rest };
    });
};
