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
import i18n from 'i18n';
import { ISSUE_TYPE } from 'project/common/issue-config';

import {
  getIssueTime,
  addFieldItem,
  updateFieldItem,
  deleteFieldItem,
  getFieldsByIssue,
  batchUpdateFieldsOrder,
  getSpecialFieldOptions,
  updateSpecialFieldOptions,
} from '../services/issue-field';

interface IState {
  issueTimeMap: ISSUE_FIELD.IIssueTime;
  fieldList: ISSUE_FIELD.IFiledItem[];
  bugStageList: ISSUE_FIELD.ISpecialOption[];
  taskTypeList: ISSUE_FIELD.ISpecialOption[];
}

const initState: IState = {
  issueTimeMap: {} as ISSUE_FIELD.IIssueTime,
  fieldList: [],
  bugStageList: [],
  taskTypeList: [],
};

const issueFieldStore = createStore({
  name: 'issueField',
  state: initState,
  effects: {
    async getIssueTime({ call, update }, payload: ISSUE_FIELD.IProjectIssueQuery) {
      const issueTimeMap = await call(getIssueTime, payload);
      update({ issueTimeMap });
    },
    async getFieldsByIssue({ call, update }, payload: ISSUE_FIELD.IFieldsByIssueQuery) {
      const fieldList = await call(getFieldsByIssue, payload);
      update({ fieldList: fieldList || [] });
      return fieldList || [];
    },
    async addFieldItem({ call }, payload: Omit<ISSUE_FIELD.IFiledItem, 'propertyID' | 'index'>) {
      return call(addFieldItem, payload, { successMsg: i18n.t('saved successfully') });
    },
    async updateFieldItem({ call }, payload: Omit<ISSUE_FIELD.IFiledItem, 'index'>) {
      return call(updateFieldItem, payload, { successMsg: i18n.t('updated successfully') });
    },
    async deleteFieldItem({ call }, payload: { propertyID: number }) {
      return call(deleteFieldItem, payload, { successMsg: i18n.t('deleted successfully') });
    },
    async batchUpdateFieldsOrder({ call, update }, payload: ISSUE_FIELD.IFiledItem[]) {
      const fieldList = await call(batchUpdateFieldsOrder, payload, { successMsg: i18n.t('updated successfully') });
      update({ fieldList });
    },
    async getSpecialFieldOptions({ call, update }, payload: ISSUE_FIELD.ISpecialFieldQuery) {
      const { issueType } = payload;
      let list = await call(getSpecialFieldOptions, payload);
      list = list || [];
      if (issueType === ISSUE_TYPE.BUG) {
        update({ bugStageList: list });
      } else if (issueType === ISSUE_TYPE.TASK) {
        update({ taskTypeList: list });
      }

      return list;
    },
    async updateSpecialFieldOptions({ call }, payload: ISSUE_FIELD.ISpecialFieldQuery) {
      const list = await call(updateSpecialFieldOptions, payload);

      return list;
    },
  },
  reducers: {
    clearFieldList(state) {
      // eslint-disable-next-line no-param-reassign
      state.fieldList = [];
    },
  },
});
export default issueFieldStore;
