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

import { isEmpty } from 'lodash';
import i18n from 'i18n';
import { message } from 'antd';
import { createStore } from 'core/cube';
import testCaseStore from 'project/stores/test-case';
import {
  createTestSet,
  getTestSetList,
  deleteTestSet,
  recoverTestSet,
  copyTestSet,
  moveTestSet,
  renameTestSet,
  deleteTestSetEntirely,
  getTestSetListInTestPlan,
} from '../services/test-set';

export interface IReloadTestSetInfo {
  parentID?: number | null;
  testSetID?: number | null;
  projectID?: number | null;
  selectProjectId?: number | null;
  isMove: boolean;
  reloadParent?: boolean; // 是否需要自己重新加载一下父级
}

interface IBreadcrumbInfo {
  // 导入导出弹框中的面包屑
  pathName: string; // 当前路径名称
  testSetID: number; // 测试集id
  testPlanID?: number | null; // 测试计划id，计划详情中可用
  selectProjectId?: number;
}

interface ITreeExtra {
  testSetID: number;
  selectProjectId?: number;
}

interface ITreeModalInfo {
  // 项目测试集弹框
  ids: number[]; // 测试集/用例的ids
  action: 'copy' | 'move' | 'recover'; // 弹框操作 copy,move,recover
  type: 'collection' | 'case' | 'multi'; // 集合 'collection', 用例 'case', 用例多选'multi'
  extra: {}; // 其他提交时需要的信息
  treeExtra: ITreeExtra; // tree 选中时保存的信息
  callback?: (data: Record<string, any>) => void; // 回调函数
}
interface IState {
  activeOuter: boolean;
  treeModalInfo: ITreeModalInfo;
  reloadTestSetInfo: IReloadTestSetInfo;
  breadcrumbInfo: IBreadcrumbInfo;
  // testSetInfo: {},
  rootTestSets: []; // 页面级别的测试集树，用于test-breadcrumb共享
  projectTestSet: TEST_SET.TestSet[];
  modalTestSet: TEST_SET.TestSet[];
  tempTestSet: TEST_SET.TestSet[];
  testSetTreeInstance: any;
}

const initReloadTestSetInfo = {
  parentId: null,
  testSetId: null,
  projectId: null,
  isMove: false,
  reloadParent: false, // 是否需要自己重新加载一下父级
};

const initTreeModalInfo: ITreeModalInfo = {
  ids: [],
  action: '',
  type: '',
  extra: {},
  treeExtra: {} as ITreeExtra,
};

const initState: IState = {
  activeOuter: false,
  treeModalInfo: initTreeModalInfo,
  reloadTestSetInfo: initReloadTestSetInfo,
  breadcrumbInfo: {
    // 导入导出弹框中的面包屑
    pathName: '', // 当前路径名称
    testSetID: 0, // 测试集id
    testPlanID: null, // 测试计划id，计划详情中可用
  },
  // testSetInfo: {},
  rootTestSets: [], // 页面级别的测试集树，用于test-breadcrumb共享
  projectTestSet: [],
  modalTestSet: [],
  tempTestSet: [],
  testSetTreeInstance: null,
};

const testSetStore = createStore({
  name: 'testSet',
  state: initState,
  effects: {
    async getProjectTestSets(
      { call, update, getParams, select },
      payload: Merge<Omit<TEST_SET.GetQuery, 'projectID'>, { mode: TEST_CASE.PageScope; forceUpdate?: boolean }>,
    ) {
      const { projectId } = getParams();
      const { mode, forceUpdate = false, ...rest } = payload;
      if (!rest.parentID) {
        rest.parentID = 0;
      }
      // 由于测试集有可能在用例导入弹框、复制/移动弹框、计划详情左侧、用例列表左侧中 被同时使用，因此树形结构交由组件自身维护
      let list = [];
      if (mode === 'testPlan' && payload.testPlanID) {
        // 测试计划测试集
        list = await call(getTestSetListInTestPlan, { parentID: payload.parentID, testPlanID: payload.testPlanID });
      } else {
        list = await call(getTestSetList, { ...rest, projectID: +projectId, recycled: false });
      }
      if (!isEmpty(list) || forceUpdate) {
        const testSet = list || [];
        if (mode === 'caseModal') {
          update({ modalTestSet: testSet });
        } else if (mode === 'temp') {
          update({ tempTestSet: testSet });
        } else {
          // When projectTestSet is empty, it will trigger to execute useEffect which is unnecessary. The specific reasons mentioned above need to be investigated.
          const prevProjectTestSet = select((s) => s.projectTestSet);
          if (isEmpty(prevProjectTestSet) && isEmpty(testSet)) return;
          update({ projectTestSet: testSet });
        }
        return testSet;
      }
      return [];
    },
    async getTestSetChildren(
      { call, getParams },
      payload: Merge<Omit<TEST_SET.GetQuery, 'projectID'>, { mode: TEST_CASE.PageScope }>,
    ) {
      const { mode, ...rest } = payload;
      const { projectId } = getParams();
      let res;
      if (mode === 'testPlan' && payload.testPlanID) {
        res = await call(getTestSetListInTestPlan, { parentID: payload.parentID, testPlanID: payload.testPlanID });
      } else {
        res = await call(getTestSetList, { ...rest, projectID: +projectId });
      }
      return res;
    },
    async renameTestSet({ call }, payload: Omit<TEST_SET.updateBody, 'moveToParentID'>) {
      const res = await call(renameTestSet, payload, { successMsg: i18n.t('dop:update completed') });
      return res;
    },
    async createTestSet({ call, getParams }, payload: Omit<TEST_SET.CreateBody, 'projectID'>) {
      const { projectId } = getParams();
      const newSet = await call(
        createTestSet,
        { ...payload, projectID: +projectId },
        { successMsg: i18n.t('dop:completed') },
      );
      return newSet;
    },
    async deleteTestSetToRecycle({ call }, testSetID: number) {
      await call(deleteTestSet, { testSetID }, { successMsg: i18n.t('deleted successfully') });
      await testCaseStore.effects.emptyListByTestSetId(testSetID);
    },
    async deleteTestSetEntirely({ call }, testSetID: number) {
      await call(deleteTestSetEntirely, { testSetID }, { successMsg: i18n.t('deleted successfully') });
      await testCaseStore.effects.emptyListByTestSetId(testSetID);
    },
    async recoverTestSet({ call, getParams }, payload: TEST_SET.RecoverQuery) {
      const { projectId } = getParams();
      await call(recoverTestSet, { ...payload, projectId });
      message.success(i18n.t('dop:restored successfully'));
      await testCaseStore.effects.emptyListByTestSetId(payload.testSetID);
    },
    async updateBreadcrumb({ update }, breadcrumbInfo: IBreadcrumbInfo) {
      // 转化为整数，这样全局统一，方便判断
      const testPlanId = parseInt(`${breadcrumbInfo.testPlanID}`, 10);
      update({
        breadcrumbInfo: {
          ...breadcrumbInfo,
          testSetID: parseInt(`${breadcrumbInfo.testSetID}`, 10),
          testPlanID: isNaN(testPlanId) ? undefined : testPlanId,
        },
      });
      testCaseStore.reducers.toggleCasePanel(false);
      testCaseStore.reducers.toggleDetailPanel({ visible: false });
    },
    // cut or copy testSets
    async subSubmitTreeCollection(
      { call, getParams },
      payload: { parentID: number; testSetID: number; action: string },
    ) {
      const { projectId: projectID } = getParams();
      const { parentID, action, testSetID } = payload;
      const isCopy = action === 'copy';
      if (isCopy) {
        await call(copyTestSet, { testSetID, copyToTestSetID: parentID });
      } else {
        await call(moveTestSet, { testSetID, moveToParentID: parentID });
      }
      message.success(i18n.t('operated successfully'));
      testSetStore.reducers.updateReloadTestSet({ isMove: !isCopy, testSetID, parentID, projectID });
    },
    // 用例批量复制，移动，恢复
    async submitMultiCases({ select }) {
      const { action, treeExtra } = select((s) => s.treeModalInfo);
      const { testSetID } = treeExtra;
      const isCopy = action === 'copy';
      const newQuery = await testCaseStore.effects.getSelectedCaseIds();
      if (isCopy) {
        await testCaseStore.effects.copyCases({ testCaseIDs: newQuery.testCaseIDs, copyToTestSetID: testSetID });
      } else {
        await testCaseStore.effects.moveCase({
          recycled: false,
          testCaseIDs: newQuery.testCaseIDs,
          moveToTestSetID: testSetID,
        });
        await testCaseStore.reducers.removeChoosenIds(newQuery.testCaseIDs);
      }
      testSetStore.reducers.closeTreeModal();
    },
    // 单个用例复制，移动, 恢复
    async submitTreeCase({ select }) {
      const { ids, action, treeExtra } = select((s) => s.treeModalInfo);
      const { testSetID } = treeExtra || {};
      const isCopy = action === 'copy';
      if (isCopy) {
        await testCaseStore.effects.copyCases({ testCaseIDs: ids, copyToTestSetID: testSetID });
      } else {
        await testCaseStore.effects.moveCase({ recycled: false, testCaseIDs: ids, moveToTestSetID: testSetID });
        await testCaseStore.reducers.removeChoosenIds(ids);
      }
      testSetStore.reducers.closeTreeModal();
    },
    async submitTreeSet({ select }) {
      const { ids, treeExtra, callback } = select((s) => s.treeModalInfo);
      const { testSetID: recoverToTestSetID } = treeExtra;
      const testSetID = ids[0];
      await testSetStore.effects.recoverTestSet({ recoverToTestSetID, testSetID });
      callback && callback({ recoverToTestSetID, testSetID });
      testSetStore.reducers.closeTreeModal();
    },
    async submitTreeModal({ select }) {
      const { type, treeExtra } = select((s) => s.treeModalInfo);
      if (isEmpty(treeExtra)) {
        message.error('project: please choose test set');
        return;
      }
      if (type === 'case') {
        // 测试用例
        await testSetStore.effects.submitTreeCase();
      } else if (type === 'multi') {
        // 用例多选
        await testSetStore.effects.submitMultiCases();
      } else if (type === 'collection') {
        await testSetStore.effects.submitTreeSet();
      }
    },
  },
  reducers: {
    updateReloadTestSet(state, reloadTestSetInfo: IReloadTestSetInfo) {
      state.reloadTestSetInfo = reloadTestSetInfo;
    },
    emptyReloadTestSet(state) {
      state.reloadTestSetInfo = initReloadTestSetInfo;
    },
    openActiveOuter(state) {
      state.activeOuter = true;
    },
    clearActiveOuter(state) {
      state.activeOuter = false;
    },
    openTreeModal(state, payload) {
      return { ...state, treeModalInfo: payload };
    },
    closeTreeModal(state) {
      state.treeModalInfo = initTreeModalInfo;
    },
    updateTreeModalExtra(state, treeExtra?: ITreeExtra) {
      state.treeModalInfo = { ...state.treeModalInfo, treeExtra: (treeExtra || {}) as ITreeExtra };
    },
  },
});

export default testSetStore;
