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

import { createStore } from 'core/cube';
import { getMessageList, getMessageStats, readOneMessage, clearAllMessage } from '../services/message';
import { PAGINATION } from 'app/constants';
import orgStore from 'app/org-home/stores/org';

export enum MSG_STATUS {
  READ = 'read',
  UNREAD = 'unread',
}

interface IState {
  list: LAYOUT.IMsg[] | [];
  detail: LAYOUT.IMsg | null;
  unreadCount: number;
  msgPaging: IPaging;
}

const initState: IState = {
  list: [],
  detail: null,
  unreadCount: 0,
  msgPaging: {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    total: 0,
  },
};

const messageStore = createStore({
  name: 'message',
  state: initState,
  effects: {
    async getMessageList({ call, update, select }, payload: { pageNo: number; pageSize?: number }) {
      try {
        const { list } = await call(getMessageList, payload, { paging: { key: 'msgPaging' } });
        const oldList: LAYOUT.IMsg[] = select((s) => s.list);
        update({ list: payload.pageNo === 1 ? list : oldList.concat(list) });
      } catch (e) {
        const prevMsgPaging = select((s) => s.msgPaging);
        update({ msgPaging: { ...prevMsgPaging, hasMore: false } });
      }
    },
    async getMessageStats({ call, update, select }) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      if (orgId) {
        const result = await call(getMessageStats);
        const unreadCount = select((s) => s.unreadCount);
        const count = result && result.unreadCount;
        update({ unreadCount: count });
        return { hasNewUnread: unreadCount < count };
      }
      return null;
    },
    async readOneMessage({ call, update, select }, id: number, hasRead: boolean) {
      const detail = await call(readOneMessage, id);
      if (detail.deduplicateId.includes('issue-')) {
        window.open(detail.content);
        if (!hasRead) {
          const list: LAYOUT.IMsg[] = select((s) => s.list);
          const newList = list.map((item) => (item.id === id ? { ...item, status: MSG_STATUS.READ } : item));
          update({ list: newList });
          await messageStore.effects.getMessageStats();
        }
        return;
      }
      if (hasRead) {
        update({ detail });
      } else {
        const list: LAYOUT.IMsg[] = select((s) => s.list);
        const newList = list.map((item) => (item.id === id ? { ...item, status: MSG_STATUS.READ } : item));
        update({ detail, list: newList });
        await messageStore.effects.getMessageStats();
      }
    },
    async clearAll({ call }) {
      await call(clearAllMessage);
      await messageStore.effects.getMessageList({ pageNo: 1 });
      await messageStore.effects.getMessageStats();
    },
  },
  reducers: {
    resetAll(state) {
      return { ...initState, unreadCount: state.unreadCount };
    },
    resetDetail(state) {
      state.detail = null;
    },
  },
});

export default messageStore;
