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

import { extend, find, includes, isBoolean, map, remove, flatMapDeep } from 'lodash';
import { message } from 'antd';
import i18n from 'i18n';
import { createStore } from 'core/cube';
import { checkNeedEmptyChoosenIds, getChoosenName, getCaseListName, formatQuery } from 'project/utils/test-case';
import { isImage, regRules, convertToFormData } from 'common/utils';
import defaultFileTypeImg from 'app/images/defaultFileImage.png';
import testSetStore, { IReloadTestSetInfo } from 'project/stores/test-set';
import testPlanStore from 'project/stores/test-plan';
import routeInfoStore from 'core/stores/route';
import { DEFAULT_FIELDS } from '../constants';

import {
  copyCases,
  create,
  deleteEntirely,
  editPartial,
  exportFileInTestCase,
  getCases,
  getCasesRelations,
  getDetail,
  getFields,
  importFileInTestCase,
  importFileInAutoTestCase,
  importFileInAutoTestCaseSet,
  updateCases,
  batchUpdateCase,
  removeRelation,
  attemptTestApi,
  addRelation,
  getDetailRelations,
  getImportExportRecords,
} from '../services/test-case';
import { TestOperation } from 'project/pages/test-manage/constants';

const tempNewCase = { case: { desc: '', bugs: [], preCondition: '', stepAndResult: [] } };

interface ICase {
  visible?: boolean;
  case: typeof tempNewCase.case;
}
interface ISelected {
  projectID?: number;
  testCaseIDs: number[];
  exclude?: number[];
  testSetID?: number;
}
interface IState {
  choosenInfo: TEST_CASE.ChoosenInfo;
  modalChoosenInfo: TEST_CASE.ChoosenInfo;
  caseAction: TestOperation; // 用例常规的批量操作
  currCase: ICase;
  caseDetail: TEST_CASE.CaseDetail;
  fields: TEST_CASE.Field[]; // 新建用例需要的字段信息
  currDetailCase: ICase;
  caseList: TEST_CASE.CaseDirectoryItem[];
  caseTotal: number;
  modalCaseList: TEST_CASE.CaseDirectoryItem[]; // 在计划详情中弹框中的 用例列表
  modalCaseTotal: number; // 在计划详情中弹框中的 用例列表总数
  metaFields: TEST_CASE.Field[]; // 后端元数据字段信息
  oldBugIds: number[]; // 详情原本的bugIds，和后面修改过的bugIds 做对比
  oldQuery: any; // 记录用例列表请求的上一个query，当前用于解决用例列表的跨多页选中效果
  issueBugs: TEST_CASE.RelatedBug[];
}

const defaultChoosenInfo: TEST_CASE.ChoosenInfo = {
  isAll: false,
  primaryKeys: [],
  exclude: [],
};
const defaultModalChoosenInfo = {
  isAll: false,
  primaryKeys: [],
  exclude: [],
};

const initState: IState = {
  choosenInfo: defaultChoosenInfo,
  modalChoosenInfo: defaultModalChoosenInfo,
  caseAction: '',
  currCase: tempNewCase,
  caseDetail: {} as TEST_CASE.CaseDetail,
  fields: [],
  currDetailCase: tempNewCase,
  caseList: [],
  caseTotal: 0,
  modalCaseList: [],
  modalCaseTotal: 0,
  metaFields: [],
  oldBugIds: [],
  oldQuery: {},
  issueBugs: [],
};

const testCaseStore = createStore({
  name: 'testCase',
  state: initState,
  effects: {
    async getCaseDetail(
      { call, update, getParams },
      payload: Merge<TEST_CASE.QueryCaseDetail, { scope: 'testPlan' | 'testCase' }>,
    ) {
      const { testPlanId } = getParams();
      let issueBugs: TEST_CASE.RelatedBug[] = [];
      let caseDetail = {} as TEST_CASE.CaseDetail;
      if (payload.scope === 'testPlan') {
        const {
          issueBugs: relationBugs,
          execStatus,
          apiCount,
          testCaseID,
          id,
        } = await call(getDetailRelations, {
          ...payload,
          testPlanID: testPlanId,
        });
        const detail = await call(getDetail, { id: testCaseID, testPlanID: testPlanId });
        caseDetail = {
          ...detail,
          id,
          planRelationCaseID: id,
          testCaseID,
          execStatus,
          apiCount,
        };
        issueBugs = relationBugs;
      } else {
        caseDetail = await call(getDetail, { ...payload, testPlanID: testPlanId });
        caseDetail = {
          testCaseID: caseDetail.id,
          ...caseDetail,
        };
      }
      update({ caseDetail, issueBugs });
      return caseDetail;
    },
    async getDetailRelations({ call, update, getParams }, payload: TEST_CASE.QueryCaseDetail) {
      const { testPlanId } = getParams();
      const { issueBugs } = await call(getDetailRelations, { ...payload, testPlanID: testPlanId });
      update({ issueBugs });
    },
    async editPartial({ call, getParams }, detailCase: TEST_CASE.CaseBody) {
      const { projectId: projectID } = getParams();
      await call(editPartial, { ...detailCase, projectID });
    },
    async getFields({ call }) {
      const fields = await call(getFields);
      testCaseStore.reducers.dealFields(fields);
    },
    async exportFile({ call }, fileType: TEST_CASE.CaseFileType) {
      const { testCaseIDs, ...rest } = await testCaseStore.effects.getSelectedCaseIds();
      const query = routeInfoStore.getState((s) => s.query);
      const temp = { recycled: query.recycled === 'true' };
      const exportQuery = formatQuery({
        ...rest,
        ...temp,
        fileType,
        testCaseID: testCaseIDs,
      }) as any as TEST_CASE.ExportFileQuery;
      return call(exportFileInTestCase, exportQuery);
    },
    async importTestCase({ call, getParams, getQuery }, payload: { file: any }) {
      const { projectId: projectID } = getParams();
      const { testSetID } = getQuery();
      const { file } = payload;
      let fileType: TEST_CASE.CaseFileType = 'excel';
      let reloadTestSetInfo = { isMove: false, testSetID: 0, parentID: 0, projectID } as IReloadTestSetInfo;
      if (regRules.xmind.pattern.test(file.name)) {
        reloadTestSetInfo = { ...reloadTestSetInfo, parentID: testSetID, testSetID };
        fileType = 'xmind';
      }
      const formData = convertToFormData({ file });
      const res = await call(importFileInTestCase, { payload: formData, query: { testSetID, projectID, fileType } });
      testCaseStore.effects.getCases();
      testSetStore.reducers.updateReloadTestSet(reloadTestSetInfo);
      return res;
    },
    async importAutoTestCase({ call, getParams }, payload: { file: File }) {
      const { projectId: projectID } = getParams();
      const { file } = payload;
      const fileType: TEST_CASE.CaseFileType = 'excel';
      const query = { projectID, fileType };
      const formData = convertToFormData({ file });
      const res = await call(importFileInAutoTestCase, { payload: formData, query });
      return res;
    },
    async importAutoTestCaseSet({ call, getParams }, payload: { file: File }) {
      const { projectId: projectID, spaceId: spaceID } = getParams();
      const { file } = payload;
      const fileType: TEST_CASE.CaseFileType = 'excel';
      const query = { projectID, fileType, spaceID };
      const formData = convertToFormData({ file });
      const res = await call(importFileInAutoTestCaseSet, { payload: formData, query });
      return res;
    },
    async create({ call, getParams }, payload: any) {
      const { projectId: strProjectId } = getParams();
      const {
        breadcrumbInfo: { testSetID, testPlanID },
      } = testSetStore.getState((s) => s);
      const projectID = parseInt(strProjectId, 10);
      const res = await call(
        create,
        {
          ...payload,
          testSetID: testSetID || 0,
          projectID,
          recycled: false,
        },
        testPlanID,
        { successMsg: i18n.t('created successfully') },
      );
      return res;
    },
    async getSelectedCaseIds({ getParams, select }, mode?: TEST_CASE.PageScope): Promise<ISelected> {
      // 用例列表中选中的测试用例
      const { projectId: projectID } = getParams();
      const testCase = select((s) => s);
      const testSet = testSetStore.getState((s) => s);
      const routeInfo = routeInfoStore.getState((s) => s);
      const { isAll, exclude, primaryKeys } = testCase[getChoosenName(mode)];
      const { testSetID } = testSet.breadcrumbInfo;
      if (mode === 'caseModal') {
        if (isAll) {
          return { exclude, testCaseIDs: primaryKeys };
        }
        return { testCaseIDs: primaryKeys };
      }
      if (isAll) {
        return { ...routeInfo.query, testSetID, projectID, testCaseIDs: primaryKeys, exclude };
      }
      return { ...routeInfo.query, projectID, testCaseIDs: primaryKeys };
    },
    async changeExecutionResult(_, status: string) {
      const { testCaseIDs: relationIDs } = await testCaseStore.effects.getSelectedCaseIds();
      const payload: Omit<TEST_PLAN.PlanBatch, 'testPlanID'> = { execStatus: status, relationIDs };
      testPlanStore.effects.updateCasesStatus(payload);
    },
    async moveCase({ call }, payload: Omit<TEST_CASE.BatchUpdate, 'priority'>) {
      const res = await call(batchUpdateCase, payload);
      testCaseStore.reducers.removeChoosenIds(payload.testCaseIDs);
      message.success(i18n.t('dop:update completed'));
      testCaseStore.effects.getCases();
      return res;
    },
    // 更新优先级
    async updatePriority({ call, select }, priority: TEST_CASE.Priority) {
      const { primaryKeys } = select((s) => s.choosenInfo);
      const payload: Omit<TEST_CASE.BatchUpdate, 'recycled' | 'moveToTestSetID'> = {
        testCaseIDs: primaryKeys,
        priority,
      };
      const res = await call(batchUpdateCase, payload);
      testCaseStore.reducers.removeChoosenIds(payload.testCaseIDs);
      message.success(i18n.t('dop:update completed'));
      testCaseStore.effects.getCases();
      return res;
    },
    // 移至回收站
    async toggleToRecycle({ call }, payload: Omit<TEST_CASE.BatchUpdate, 'priority'>) {
      const res = await call(batchUpdateCase, payload);
      testCaseStore.reducers.removeChoosenIds(payload.testCaseIDs);
      message.success(i18n.t('deleted successfully'));
      testCaseStore.effects.getCases();
      return res;
    },
    async deleteEntirely({ call }, id?: number) {
      let tempIds = [];
      if (id) {
        // 单条
        tempIds = [id];
        await call(deleteEntirely, { testCaseIDs: tempIds });
      } else {
        // 批量
        const newQuery = await testCaseStore.effects.getSelectedCaseIds();
        tempIds = newQuery.testCaseIDs || [];
        await call(deleteEntirely, { testCaseIDs: tempIds });
      }
      testCaseStore.reducers.removeChoosenIds(tempIds);
      message.success(i18n.t('deleted successfully'));
      testCaseStore.effects.getCases();
    },
    async emptyListByTestSetId({ update }, targetTestSetId: number) {
      const { testSetID } = testSetStore.getState((s) => s.breadcrumbInfo);
      if (targetTestSetId !== testSetID) {
        return;
      }
      update({ caseList: [], caseTotal: 0 });
      testCaseStore.reducers.triggerChoosenAll({ isAll: false, scope: 'testCase' });
    },
    async updateCases({ call }, { query, payload }: { query: TEST_CASE.CaseFilter; payload: TEST_CASE.CaseBodyPart }) {
      await call(updateCases, { query, payload });
      message.success(i18n.t('updated successfully'));
      testCaseStore.effects.getCases();
    },
    async copyCases({ call, getParams }, payload: Omit<TEST_CASE.BatchCopy, 'projectID'>) {
      const { projectId } = getParams();
      await call(copyCases, { ...payload, projectID: +projectId });
      message.success(i18n.t('copied successfully'));
      testCaseStore.effects.getCases();
    },
    async getCases(
      { call, select, update, getParams },
      payload?: Merge<TEST_CASE.QueryCase, { scope: TEST_CASE.PageScope }>,
    ) {
      const { scope, ...rest } = payload || ({} as Merge<TEST_CASE.QueryCase, { scope: TEST_CASE.PageScope }>);
      const breadcrumbInfo = testSetStore.getState((s) => s.breadcrumbInfo);
      const oldQuery = select((s) => s.oldQuery);
      const query = routeInfoStore.getState((s) => s.query);
      const { projectId: projectID, testPlanId } = getParams();
      let { testSetID } = breadcrumbInfo;
      // 先取传过来的，再取url上的
      if (rest.testSetID !== undefined) {
        testSetID = rest.testSetID;
      } else if (query.testSetID !== undefined) {
        testSetID = query.testSetID;
      }
      // 1、当筛选器、表格的page、sorter发生变更时
      // 2、及时性的筛选信息
      if (scope === 'caseModal') {
        // 如果是在计划详情中的用例弹框时，传入的参数覆盖url上的参数
        const newQuery = { ...query, ...rest, testSetID, projectID, query: undefined }; // Set the query outside the modal to undefined, prevent to filter modal data
        const { testSets, total } = await call(getCases, formatQuery({ pageSize: 15, ...newQuery }));
        update({ modalCaseList: testSets, modalCaseTotal: total });
        testCaseStore.reducers.triggerChoosenAll({ isAll: false, scope });
        return;
      }
      const fetchData = testPlanId ? getCasesRelations : getCases;
      const newQuery = { ...rest, testSetID, ...query, projectID, testPlanID: testPlanId };
      const { testSets, total } = await call(fetchData, formatQuery({ pageSize: 15, ...newQuery }));
      update({ caseList: testSets, caseTotal: total, oldQuery: newQuery });
      if (checkNeedEmptyChoosenIds(newQuery, oldQuery)) {
        testCaseStore.reducers.triggerChoosenAll({ isAll: false, scope });
      }
    },
    async attemptTestApi({ call }, payload: TEST_CASE.TestApi) {
      const result = await call(attemptTestApi, payload);
      return result;
    },
    async removeRelation({ call, getParams }, payload: Omit<TEST_CASE.RemoveRelation, 'testPlanID'>) {
      const { testPlanId } = getParams();
      const res = await call(
        removeRelation,
        { ...payload, testPlanID: testPlanId },
        { successMsg: i18n.t('dop:disassociated successfully') },
      );
      return res;
    },
    async addRelation({ call, getParams }, payload: Omit<TEST_CASE.AddRelation, 'testPlanID'>) {
      const { testPlanId } = getParams();
      const res = await call(
        addRelation,
        { ...payload, testPlanID: testPlanId },
        { successMsg: i18n.t('dop:associated successfully') },
      );
      return res;
    },
    async getImportExportRecords({ call, getParams }, types: TEST_CASE.ImportOrExport[]) {
      const { projectId } = getParams();
      return call(getImportExportRecords, { projectId, types });
    },
  },
  reducers: {
    openNormalModal(state, caseAction) {
      state.caseAction = caseAction;
    },
    closeNormalModal(state) {
      state.caseAction = '';
    },
    triggerChoosenAll(state, { isAll, scope }: { isAll: boolean; scope: TEST_CASE.PageScope }) {
      if (scope === 'temp') {
        return;
      }
      const keyName = getChoosenName(scope);
      const listName = getCaseListName(scope);
      let nextIsAll = isAll;
      if (!isBoolean(isAll)) {
        nextIsAll = !state[keyName].isAll;
      }
      let testCaseIDs: number[] = [];
      if (nextIsAll) {
        testCaseIDs = flatMapDeep(state[listName], ({ testCases }) => flatMapDeep(testCases, 'id'));
      }
      state[keyName] = { isAll: nextIsAll, exclude: [], primaryKeys: nextIsAll ? testCaseIDs : [] };
    },
    removeChoosenIds(state, ids) {
      if (!ids) {
        return;
      }
      const { primaryKeys } = state.choosenInfo;
      remove(primaryKeys, (caseId) => includes(ids, caseId));
      state.choosenInfo = { ...state.choosenInfo, isAll: false, primaryKeys };
    },
    clearChoosenInfo(state, { mode }: { mode: TEST_CASE.PageScope }) {
      const keyName = getChoosenName(mode);
      state[keyName] = {
        isAll: false,
        primaryKeys: [],
        exclude: [],
      };
    },
    updateChoosenInfo(state, { id, mode, checked }) {
      const keyName = getChoosenName(mode);
      const { isAll, primaryKeys } = state[keyName];
      const copy = [...primaryKeys];
      let nextIsAll = isAll;
      if (checked) {
        const listName = getCaseListName(mode);
        const caseCount = flatMapDeep(state[listName], ({ testCases }) => flatMapDeep(testCases, 'id')).length;
        copy.push(id);
        nextIsAll = copy.length === caseCount;
      } else {
        nextIsAll = false;
        if (includes(copy, id)) {
          // 已经选中时
          remove(copy, (caseId) => caseId === id);
        }
      }
      state[keyName] = { isAll: nextIsAll, exclude: [], primaryKeys: [...copy] };
    },
    resetStore() {
      return initState;
    },
    toggleCasePanel(state, visible) {
      state.currCase = { ...state.currCase, visible };
      state.currDetailCase = { ...state.currDetailCase, visible: false };
    },
    toggleDetailPanel(state, { visible, detailCase }) {
      state.currDetailCase = { visible, case: { ...state.currDetailCase.case, ...detailCase } };
      state.currCase = { ...state.currCase, visible: false };
    },
    setCurrCase(state, currCase) {
      state.currCase = { ...state.currCase, case: { ...currCase } };
    },
    setDetailCase(state, detailCase) {
      state.currDetailCase = { ...state.currDetailCase, case: { ...detailCase } };
    },
    dealFields(state, fields) {
      let fieldsVal = [...DEFAULT_FIELDS];
      fieldsVal = map(fieldsVal, (defaultField) => {
        const metaField = find(fields, (filedItem) => filedItem.uniqueName === defaultField.uniqueName);
        if (metaField) {
          return metaField;
        }
        return defaultField;
      });
      state.fields = fieldsVal;
      state.metaFields = fields;
    },
    dealDetail(state, { payload }) {
      const { detailCase } = payload;
      const { attachmentObjects: attachments } = detailCase;
      attachments.map((value: any) => {
        if (!isImage(value.name)) {
          // eslint-disable-next-line no-param-reassign
          value.thumbUrl = defaultFileTypeImg;
        }
        extend(value, {
          uid: value.id,
          name: value.name,
          status: 'done',
          url: value.url,
          response: [{ ...value }],
          thumbUrl: value.thumbUrl || undefined,
        });
        return value;
      });
      detailCase.attachments = attachments;
      const desc = detailCase.desc && detailCase.desc !== '<p></p>' ? detailCase.desc : '';
      const preCondition =
        detailCase.preCondition && detailCase.preCondition !== '<p></p>' ? detailCase.preCondition : '';
      detailCase.desc = desc;
      detailCase.preCondition = preCondition;
      state.currDetailCase = {
        ...state.currDetailCase,
        case: { ...detailCase, descIssues: detailCase.descIssues || [] },
      };
      state.oldBugIds = detailCase.bugs && detailCase.bugs.map(({ id }: any) => id);
    },
    clearCurrCase(state) {
      state.currCase = tempNewCase;
    },
    clearCurrDetailCase(state) {
      state.currDetailCase = tempNewCase;
    },
    clearCaseDetail(state) {
      state.caseDetail = {} as TEST_CASE.CaseDetail;
    },
  },
});

export default testCaseStore;
