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

import { getDefaultPaging } from 'common/utils';
import { createStore } from 'core/cube';
import { getTaskList } from '../services/task';

interface IState {
  list: CLUSTER_TASK.Detail[];
  paging: IPaging;
}

const initState: IState = {
  list: [],
  paging: getDefaultPaging(),
};

const task = createStore({
  name: 'clusterTask',
  state: initState,
  effects: {
    async getTaskList({ call, update }, payload: CLUSTER_TASK.ListQuery) {
      const result = await call(getTaskList, payload, { paging: { key: 'paging' } });
      update({ list: result.list });
    },
  },
  reducers: {
    resetState() {
      return initState;
    },
  },
});

export default task;
