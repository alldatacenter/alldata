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
import i18n from 'i18n';
import { getApprovalList, updateApproval } from '../services/approval';

interface IState {
  doneList: APPROVAL.Item[];
  donePaging: IPaging;
  undoneList: APPROVAL.Item[];
  undonePaging: IPaging;
  userIDs: string[];
}

const initState: IState = {
  doneList: [],
  donePaging: getDefaultPaging(),
  undoneList: [],
  undonePaging: getDefaultPaging(),
  userIDs: [],
};

const approval = createStore({
  name: 'orgApproval',
  state: initState,
  effects: {
    async getApprovalList({ call, update }, payload: Merge<APPROVAL.IApprovalQuery, { type: APPROVAL.ApprovalType }>) {
      const { type, status: _status, ...rest } = payload;
      const [listKey, pagingKey] = {
        done: ['doneList', 'donePaging'],
        undone: ['undoneList', 'undonePaging'],
      }[type];
      let status = _status as string | string[];
      if (type === 'done' && !status) status = ['denied', 'approved'];
      const { data, userIDs = [] } = await call(
        getApprovalList,
        { status, ...rest },
        { paging: { key: pagingKey }, fullResult: true },
      );
      update({ [`${listKey}`]: data.list, userIDs });
      return { ...data };
    },
    async updateApproval({ call }, payload: APPROVAL.UpdateBody) {
      const res = await call(updateApproval, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
  },
  reducers: {
    clearApprovalList(state, type: APPROVAL.ApprovalType) {
      const [listKey, pagingKey] = {
        done: ['doneList', 'donePaging'],
        undone: ['undoneList', 'undonePaging'],
      }[type];
      state[listKey] = [];
      state[pagingKey] = getDefaultPaging();
    },
  },
});

export default approval;
