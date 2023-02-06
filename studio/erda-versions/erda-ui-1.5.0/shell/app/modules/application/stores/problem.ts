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
import { getUserMap } from 'core/stores/userMap';
import { getDefaultPaging } from 'common/utils';
import {
  getTicketList,
  getTicketDetail,
  addTicket,
  closeTicket,
  reopenTicket,
  getComments,
  createComment,
} from '../services/problem';
import i18n from 'i18n';
import { map } from 'lodash';
import userStore from 'app/user/stores';

const getUser = (user: ILoginUser) =>
  user ? user.nick || user.name || user.email || user.phone || user.id : i18n.t('dop:system');

interface IState {
  ticketList: PROBLEM.Ticket[];
  paging: IPaging;
  comments: PROBLEM.Comment[];
  commentsTotal: number;
  openTotal: number;
  detail: PROBLEM.Ticket;
}

const initState: IState = {
  ticketList: [],
  paging: getDefaultPaging(),
  comments: [],
  commentsTotal: 0,
  openTotal: 0,
  detail: {} as PROBLEM.Ticket,
};

const ticketStore = createStore({
  name: 'appTicket',
  state: initState,
  effects: {
    async getTicketList({ call, update }, payload: Merge<PROBLEM.ListQuery, { filters: Obj }>) {
      const { list = [], total } = await call(getTicketList, payload, {
        paging: { key: 'paging', listKey: 'tickets' },
      });

      const userMap = getUserMap();
      const ticketList = map(list, (ticket) => {
        // 在userInfo中获取对应的用户名
        const { creator, lastOperator, ...other } = ticket;
        return {
          ...other,
          lastOperator,
          creator: getUser(userMap[creator]),
          lastOperatorUser: getUser(userMap[lastOperator]),
        };
      });

      if (payload.status === 'open') {
        update({ openTotal: total });
      }
      update({ ticketList });
      return { list, total };
    },
    async getTicketDetail({ call, update, getParams }) {
      const { ticketId } = getParams();
      const detail = await call(getTicketDetail, ticketId);
      const userMap = getUserMap();
      detail.author = getUser(userMap[detail.creator]);
      detail.lastOperator = getUser(userMap[detail.lastOperator]);

      update({ detail });
    },
    async getTicketComments({ call, update, getParams }) {
      const { ticketId } = getParams();
      const { comments: list, total } = await call(getComments, ticketId);
      const userMap = getUserMap();

      list.forEach((comment) => {
        if (userMap[comment.userID]) {
          // eslint-disable-next-line no-param-reassign
          comment.author = getUser(userMap[comment.userID]);
        }
      });
      update({ comments: list, commentsTotal: total });
    },
    async createTicketComments({ call, getParams }, payload: Omit<PROBLEM.CommentBody, 'ticketID' | 'userID'>) {
      const { ticketId } = getParams();
      const { loginUser } = userStore.getState((s) => s);
      await call(createComment, {
        ...payload,
        ticketID: parseInt(ticketId, 10),
        userID: loginUser.id,
      });

      await ticketStore.effects.getTicketComments();
    },
    async closeTicket({ call, getParams }) {
      const { ticketId } = getParams();
      await call(closeTicket, parseInt(ticketId, 10));
      await ticketStore.effects.getTicketDetail();
    },
    async addTicket({ call }, payload: PROBLEM.CreateBody) {
      const { loginUser } = userStore.getState((s) => s);
      await call(addTicket, { ...payload, userID: loginUser.id });
    },
    async reopenTicket({ call, getParams }) {
      const { ticketId } = getParams();
      await call(reopenTicket, ticketId);
    },
  },
  reducers: {
    clearTicketDetail(state) {
      state.detail = {} as PROBLEM.Ticket;
    },
    clearTicketList(state) {
      state.ticketList = [];
    },
  },
});

export default ticketStore;
