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

import { createFlatStore } from 'core/cube';
import i18n from 'i18n';
import { addBranchRule, getBranchRules, deleteBranchRule, updateBranchRule } from '../services/branch-rule';

interface IState {
  branchRules: PROJECT.IBranchRule[];
}

const initState: IState = {
  branchRules: [],
};

const branchRule = createFlatStore({
  name: 'branchRule',
  state: initState,
  effects: {
    async getBranchRules({ call, update }, payload: { scopeId: number; scopeType: string }) {
      const branchRules = await call(getBranchRules, payload);
      update({ branchRules });
    },
    async addBranchRule({ call }, payload: PROJECT.IBranchRuleCreateBody) {
      await call(addBranchRule, { ...payload }, { successMsg: i18n.t('added successfully') });
    },
    async deleteBranchRule({ call }, { id }: { id: number }) {
      await call(deleteBranchRule, { id }, { successMsg: i18n.t('deleted successfully') });
    },
    async updateBranchRule({ call }, payload: PROJECT.IBranchRule) {
      await call(updateBranchRule, payload, { successMsg: i18n.t('updated successfully') });
    },
  },
  reducers: {
    clearBranchRule(state) {
      state.branchRules = [];
    },
  },
});

export default branchRule;
