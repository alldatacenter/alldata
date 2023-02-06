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

import orgStore from 'app/org-home/stores/org';
import routeInfoStore from 'core/stores/route';
import { createFlatStore } from 'core/cube';
import { map, uniqueId } from 'lodash';
import i18n from 'i18n';
import {
  createRule,
  deleteRule,
  editRule,
  getAddonLogs,
  getAddonLogStatistics,
  getLogs,
  getLogStatistics,
  getRule,
  getRules,
  getRuleTemplate,
  getRuleTemplates,
  getTagsTree,
  testRule,
  toggleRule,
} from 'msp/services/log-analyze';
import mspStore from 'msp/stores/micro-service';

interface IState {
  tagsTree: LOG_ANALYZE.TagsTree[];
  logs: LOG_ANALYZE.Log[];
  logStatistics: LOG_ANALYZE.LogStatistics;
  rules: LOG_ANALYZE.RuleListItem[];
  templates: LOG_ANALYZE.RuleTemplate[];
  curRule: LOG_ANALYZE.Rule;
}

const initState: IState = {
  tagsTree: [],
  logs: [],
  logStatistics: {} as LOG_ANALYZE.LogStatistics,
  rules: [],
  templates: [],
  curRule: {} as LOG_ANALYZE.Rule,
};

const getScope = (useTk?: boolean) => {
  const [routeMarks, params] = routeInfoStore.getState((s) => [s.routeMarks, s.params]);
  const orgName = orgStore.getState((s) => s.currentOrg.name);
  const inCmp = routeMarks.includes('cmp');
  const inMsp = routeMarks.includes('msp');
  const msMenuMap = mspStore.getState((s) => s.msMenuMap);
  const scope = inCmp ? 'org' : inMsp ? 'micro-service' : '';
  const scopeID = inCmp ? orgName : inMsp ? (useTk ? msMenuMap.LogAnalyze?.params.terminusKey : params.addonId) : '';
  return { scope, scopeID };
};

const LogAnalyze = createFlatStore({
  name: 'logAnalyze',
  state: initState,
  effects: {
    async getTagsTree({ call, update }) {
      const tagsTree = await call(getTagsTree(getScope()));
      update({ tagsTree });
      return tagsTree;
    },
    async getLogs({ call, update }, payload: LOG_ANALYZE.GetLogQuery) {
      const { data: logs = [] } = (await call(getLogs, payload)) || {};
      update({ logs: map(logs, (log) => ({ ...log, uniId: uniqueId() })) });
    },
    async getLogStatistics({ call, update }, payload: LOG_ANALYZE.GetLogQuery) {
      const logStatistics = await call(getLogStatistics, payload);
      update({ logStatistics });
    },
    async getAddonLogs({ call, update }, payload: LOG_ANALYZE.AddonSearchQuery) {
      const { data: logs = [] } = (await call(getAddonLogs, payload)) || {};
      update({ logs: map(logs, (log) => ({ ...log, uniId: uniqueId() })) });
    },
    async getAddonLogStatistics({ call, update }, payload: LOG_ANALYZE.AddonSearchQuery) {
      const logStatistics = await call(getAddonLogStatistics, payload);
      update({ logStatistics });
    },
    async getRules({ call, update }) {
      const rules = await call(getRules(getScope(true)));
      update({ rules });
    },
    async getRule({ call, update }, id: string) {
      const curRule = await call(getRule(getScope(true)), id);
      update({ curRule });
    },
    async getRuleTemplates({ call, update }) {
      const templates = await call(getRuleTemplates(getScope(true)));
      update({ templates });
    },
    async getRuleTemplate({ call }, template: string) {
      const result: LOG_ANALYZE.Template = await call(getRuleTemplate(getScope(true)), template);
      return result;
    },
    async createRule({ call }, payload: LOG_ANALYZE.Rule) {
      await call(createRule(getScope(true)), payload, { successMsg: i18n.t('added successfully') });
    },
    async editRule({ call }, payload: LOG_ANALYZE.Rule) {
      await call(editRule(getScope(true)), payload, { successMsg: i18n.t('edited successfully') });
    },
    async toggleRule({ call, select, update }, payload: { id: number; enable: boolean }) {
      await call(toggleRule(getScope(true)), payload, { successMsg: i18n.t('operated successfully') });
      const newRule = select((s) => s.rules).map((rule) => {
        return {
          ...rule,
          enable: rule.id === payload.id ? payload.enable : rule.enable,
        };
      });
      update({ rules: newRule });
    },
    async deleteRule({ call }, id: number) {
      await call(deleteRule(getScope(true)), id, { successMsg: i18n.t('deleted successfully') });
      LogAnalyze.getRules();
    },
    async testRule({ call }, payload: LOG_ANALYZE.TestRuleQuery) {
      const { fields = [] } = (await call(testRule(getScope(true)), payload)) || {};
      return fields;
    },
  },
  reducers: {
    clearLogs(state) {
      state.logs = initState.logs;
    },
    clearLogStatistics(state) {
      state.logStatistics = initState.logStatistics;
    },
    clearCurRule(state) {
      state.curRule = {} as LOG_ANALYZE.Rule;
    },
  },
});

export default LogAnalyze;
