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
import { getDefaultPaging } from 'common/utils';
import projectLabelStore from 'project/stores/label';
import {
  getProjectIterations,
  createProjectIteration,
  editProjectIteration,
  deleteIteration,
  getIterationDetail,
} from '../services/project-iteration';
import { isEmpty, map } from 'lodash';
import userStore from 'app/user/stores';
import { ISSUE_OPTION } from 'project/common/components/issue/issue-config';
import { getIssues, createIssue } from '../services/issue';
import moment from 'moment';
import i18n from 'i18n';
import breadcrumbStore from 'layout/stores/breadcrumb';

interface IState {
  iterationList: ITERATION.Detail[];
  iterationDetail: ITERATION.Detail;
  iterationPaging: IPaging;
  undoneIterations: ITERATION.Detail[];
  issuesMap: Merge<Obj<ISSUE.Issue[]>, { [k: string]: any }>;
  backlogIssues: ISSUE.Issue[];
  backlogIssuesPaging: IPaging;
}

const initState: IState = {
  iterationList: [],
  iterationDetail: {} as ITERATION.Detail,
  iterationPaging: getDefaultPaging(),
  undoneIterations: [],
  issuesMap: {},
  backlogIssues: [],
  backlogIssuesPaging: getDefaultPaging(),
};

const iteration = createStore({
  name: 'iteration',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering, isLeaving, params }) => {
      if (isEntering('iteration')) {
        projectLabelStore.effects.getLabels({ type: 'issue' });
      } else if (isLeaving('iteration')) {
        projectLabelStore.reducers.clearList();
      }
      if (isEntering('iterationDetail')) {
        iteration.effects.getIterationDetail(+params.iterationId).then((detail) => {
          breadcrumbStore.reducers.setInfo('iterationName', detail.title);
        });
      }
    });
  },
  effects: {
    async getIterations({ call, update }, payload: ITERATION.ListQuery) {
      const { list = [] } = await call(getProjectIterations, payload, { paging: { key: 'iterationPaging' } });
      update({ iterationList: list });
      return list;
    },
    async getIterationDetail({ call, update, getParams }, id: number) {
      const { projectId: projectID } = getParams();
      const detail = await call(getIterationDetail, { id, projectID });
      update({ iterationDetail: detail });
      return detail;
    },
    async createIteration({ call, getParams }, payload: ITERATION.CreateBody) {
      const { projectId } = getParams();
      return call(
        createProjectIteration,
        { ...payload, projectID: +projectId, state: 'UNFILED' },
        { successMsg: i18n.t('created successfully') },
      );
    },
    async editIteration({ call, getParams }, payload: Omit<ITERATION.UpdateBody, 'projectID'>) {
      const { projectId } = getParams();
      return call(
        editProjectIteration,
        { ...payload, projectID: +projectId },
        { successMsg: i18n.t('updated successfully') },
      );
    },
    async deleteIteration({ call }, id: number) {
      await call(deleteIteration, id, { successMsg: i18n.t('deleted successfully') });
    },
    async getUndoneIterations({ call, getParams, update }) {
      const { projectId: projectID } = getParams();
      const { list = [] } = await call(getProjectIterations, { projectID, pageSize: 100, deadline: moment().format() });
      update({ undoneIterations: list });
      return list;
    },
    async getIterationsIssues(
      { call, update, select, getParams },
      { iterationId, pageNo }: { iterationId: number; pageNo: number },
    ) {
      const preIssuesMap = select((s) => s.issuesMap);
      const { projectId: projectID } = getParams();
      const { list = [], total } = await call(getIssues, { iterationID: iterationId, projectID, pageNo });
      update({ issuesMap: { ...preIssuesMap, [iterationId]: { list, total } } });
      return list;
    },
    async getBacklogIssues({ call, update, select, getParams }, payload: ISSUE.BacklogIssueQuery) {
      const { projectId: projectID } = getParams();
      const type = isEmpty(payload.type) ? map(ISSUE_OPTION) : payload.type;
      const { loadMore, ...rest } = payload || {};

      const query = { ...rest, type, iterationID: -1, projectID } as ISSUE.IssueListQuery;
      const { list = [] } = await call(getIssues, query, { paging: { key: 'backlogIssuesPaging' } });

      let backlogIssues = select((state) => state.backlogIssues);
      const backlogIssuesPaging = select((state) => state.backlogIssuesPaging);
      if (loadMore && backlogIssuesPaging.pageNo !== 1) {
        backlogIssues = backlogIssues.concat(list);
      } else {
        backlogIssues = list;
      }

      update({ backlogIssues });
      return list;
    },
    async createIssue({ call, getParams }, payload: ISSUE.BacklogIssueCreateBody) {
      const { projectId: projectID } = getParams();
      const userId = userStore.getState((s) => s.loginUser.id);
      const res = await call(
        createIssue,
        { assignee: userId, iterationID: -1, projectID: +projectID, ...payload } as ISSUE.Issue,
        { successMsg: i18n.t('created successfully') },
      );
      return res;
    },
  },
  reducers: {
    updateIterationList(state, list: ITERATION.Detail[]) {
      state.iterationList = list;
    },
    clearIterationList(state) {
      state.iterationList = [];
      state.iterationPaging = getDefaultPaging();
    },
    clearIterationDetail(state) {
      state.iterationDetail = {} as ITERATION.Detail;
    },
    clearBacklogIssues(state) {
      state.backlogIssues = [];
      state.backlogIssuesPaging = getDefaultPaging();
    },
    clearUndoneIterations(state) {
      state.undoneIterations = [];
    },
  },
});

export default iteration;
