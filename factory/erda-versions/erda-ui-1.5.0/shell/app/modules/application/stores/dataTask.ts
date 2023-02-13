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

import {
  getBusinessScope,
  batchCreateTask,
  getWorkFlowFiles,
  getStarChartSource,
  getTableAttrs,
  getOutputTables,
  getBusinessProcesses,
} from '../services/dataTask';
import i18n from 'i18n';
import appStore from 'application/stores/application';
import routeInfoStore from 'core/stores/route';
import { eventHub } from 'common/utils/event-hub';
import { createStore } from 'core/cube';
import { PAGINATION } from 'app/constants';
import { isEmpty } from 'lodash';

const getAppDetail = () =>
  new Promise((resolve) => {
    const { appId } = routeInfoStore.getState((s) => s.params);
    let appDetail = appStore.getState((s) => s.detail);
    const notSameApp = appId && String(appId) !== String(appDetail.id);
    if (!appId || notSameApp) {
      eventHub.once('appStore/getAppDetail', () => {
        appDetail = appStore.getState((s) => s.detail);
        resolve(appDetail);
      });
    } else {
      resolve(appDetail);
    }
  });

const dataTask = createStore({
  name: 'dataTask',
  state: {
    workFlowFiles: [] as any[],
    modelBusinessScope: {} as Obj,
    marketBusinessScope: {} as Obj,
    businessProcessList: [] as any[],
    businessProcessPaging: {} as any,
    outputTableList: [] as any[],
    outputTablePaging: [] as any,
    tableAttrsList: [] as any[],
    tableAttrsPaging: [] as any,
  },
  effects: {
    async getWorkFlowFiles({ call, update }) {
      const { gitRepoAbbrev } = appStore.getState((s) => s.detail);
      if (gitRepoAbbrev) {
        let workFlowFiles = await call(getWorkFlowFiles, { gitRepoAbbrev });
        workFlowFiles = workFlowFiles && workFlowFiles.map((file: any) => ({ title: file.name }));
        update({ workFlowFiles });
      }
    },
    async batchCreateTask({ call, getParams }, payload) {
      const { appId } = getParams();
      const result = await call(
        batchCreateTask,
        { ...payload, appId },
        { successMsg: i18n.t('dop:start executing the build') },
      );
      return result;
    },
    async getBusinessScope({ call, update }, payload) {
      const { compName } = payload;
      let { gitRepo } = appStore.getState((s) => s.detail);
      if (!gitRepo) {
        await getAppDetail();
        const appDetail = appStore.getState((s) => s.detail);
        gitRepo = appDetail.gitRepo;
      }
      const businessScope = await call(getBusinessScope, { remoteUri: gitRepo });
      compName === 'model'
        ? update({ modelBusinessScope: businessScope })
        : update({ marketBusinessScope: businessScope });
    },
    async getBusinessProcesses({ call, update, select }, payload) {
      const { gitRepo } = appStore.getState((s) => s.detail);
      const [originalList, businessProcessPaging] = select((s) => [s.businessProcessList, s.businessProcessPaging]);

      const { pageNo = 1, pageSize = PAGINATION.pageSize, searchKey, ...rest } = payload;
      const params = !isEmpty(searchKey) ? { pageNo, pageSize, keyWord: searchKey } : { pageNo, pageSize };
      try {
        const { list, total } =
          (await call(
            getBusinessProcesses,
            { ...params, ...rest, remoteUri: gitRepo },
            { paging: { key: 'businessProcessPaging' } },
          )) || {};
        let newList = list;
        if (pageNo !== 1) {
          newList = originalList.concat(list);
        }
        update({ businessProcessList: newList });
        return { total, list: newList };
      } catch (e) {
        update({ businessProcessPaging: { ...businessProcessPaging, hasMore: false } });
        return { total: 0, list: [] };
      }
    },
    async getOutputTables({ call, update, select }, payload) {
      const { gitRepo } = appStore.getState((s) => s.detail);
      const [originalList, outputTablePaging] = select((s) => [s.outputTableList, s.outputTablePaging]);

      const { pageNo = 1, pageSize = PAGINATION.pageSize, searchKey, ...rest } = payload;
      const params = !isEmpty(searchKey) ? { pageNo, pageSize, keyWord: searchKey } : { pageNo, pageSize };
      try {
        const { list, total } =
          (await call(
            getOutputTables,
            { ...params, ...rest, remoteUri: gitRepo },
            { paging: { key: 'outputTablePaging' } },
          )) || {};
        let newList = originalList;
        if (pageNo !== 1) {
          newList = originalList.concat(list);
        }
        update({ outputTableList: newList });
        return { total, list: newList };
      } catch (error) {
        update({ outputTablePaging: { ...outputTablePaging, hasMore: false } });
        return { total: 0, list: [] };
      }
    },
    async getTableAttrs({ call, update }, payload) {
      const { list: tableAttrsList = [], total } = await call(getTableAttrs, payload, {
        paging: { key: 'tableAttrsPaging' },
      });
      update({ tableAttrsList });
      return { total, list: tableAttrsList };
    },
    async getStarChartSource({ call, getParams }) {
      const { filePath } = getParams();
      const startChartSource = await call(getStarChartSource, { filePath });
      return startChartSource;
    },
  },
  reducers: {
    clearWorkFlowFiles(state) {
      state.workFlowFiles = [];
    },
    clearBusinessScope(state, payload) {
      const { compName } = payload;
      if (compName === 'model') {
        state.modelBusinessScope = {};
      } else {
        state.marketBusinessScope = {};
      }
    },
    clearBusinessProcesses(state) {
      state.businessProcessList = [];
    },
    clearOutputTables(state) {
      state.outputTableList = [];
    },
  },
});

export default dataTask;
