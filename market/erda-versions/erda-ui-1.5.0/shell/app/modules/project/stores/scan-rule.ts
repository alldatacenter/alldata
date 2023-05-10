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
import {
  getAppendedScanRules,
  updateScanRule,
  getOptionalScanRules,
  deleteScanRule,
  batchDeleteScanRule,
  batchInsertScanRule,
} from '../services/scan-rule';
import i18n from 'i18n';

interface IState {
  appendedScanRules: SCAN_RULE.AppendedItem[];
  optionalScanRules: SCAN_RULE.AppendedItem[];
}

const initState: IState = {
  appendedScanRules: [],
  optionalScanRules: [],
};

const scanRuleStore = createFlatStore({
  name: 'scanRule',
  state: initState,
  effects: {
    async getAppendedScanRules({ call, update }, payload: SCAN_RULE.IAppendedQuery) {
      const { list = [] } = await call(getAppendedScanRules, payload);
      update({ appendedScanRules: list });
    },

    async deleteScanRule({ call }, payload: SCAN_RULE.IDeleteBody) {
      await call(deleteScanRule, payload, { successMsg: i18n.t('deleted successfully') });
    },

    async batchDeleteScanRule({ call }, payload: SCAN_RULE.IBatchDeleteBody) {
      await call(batchDeleteScanRule, payload, { successMsg: i18n.t('deleted successfully') });
    },

    async updateScanRule({ call }, payload: SCAN_RULE.IUpdateBody) {
      await call(updateScanRule, payload, { successMsg: i18n.t('updated successfully') });
    },

    async batchInsertScanRule({ call }, payload: SCAN_RULE.IBatchInsertBody) {
      await call(batchInsertScanRule, payload, { successMsg: i18n.t('updated successfully') });
    },

    async getOptionalScanRules({ call, update }, payload: SCAN_RULE.IOptionalQuery) {
      const list = await call(getOptionalScanRules, payload);
      update({ optionalScanRules: list });
    },
  },
});

export default scanRuleStore;
