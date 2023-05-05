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

import agent from 'agent';

export const getTagsTree =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (): LOG_ANALYZE.TagsTree[] => {
    return agent
      .get(`/api/${scope}/logs/tags/tree`)
      .query({ scopeID })
      .then((response: any) => response.body);
  };

export const getLogStatistics = (query: LOG_ANALYZE.GetLogQuery): LOG_ANALYZE.LogStatistics => {
  return agent
    .get('/api/org/logs/statistic/histogram')
    .query(query)
    .then((response: any) => response.body);
};

export const getLogs = (query: LOG_ANALYZE.GetLogQuery): { data: LOG_ANALYZE.Log[]; total: number } => {
  return agent
    .get('/api/org/logs/search')
    .query(query)
    .then((response: any) => response.body);
};

export const getAddonLogStatistics = ({
  addonID,
  ...query
}: LOG_ANALYZE.AddonSearchQuery): LOG_ANALYZE.LogStatistics => {
  return agent
    .get(`/api/log-analytics/${addonID}/statistic`)
    .query(query)
    .then((response: any) => response.body);
};

export const getAddonLogs = ({
  addonID,
  ...query
}: LOG_ANALYZE.AddonSearchQuery): { data: LOG_ANALYZE.Log[]; total: number } => {
  return agent
    .get(`/api/log-analytics/${addonID}/search`)
    .query(query)
    .then((response: any) => response.body);
};

export const getRuleTemplates =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (): LOG_ANALYZE.RuleTemplate[] => {
    return agent
      .get(`/api/${scope}/logs/rules/templates`)
      .query({ scopeID })
      .then((response: any) => response.body);
  };

export const getRuleTemplate =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (template: string): LOG_ANALYZE.Template => {
    return agent
      .get(`/api/${scope}/logs/rules/templates/${template}`)
      .query({ scopeID })
      .then((response: any) => response.body);
  };

export const getRules =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (): LOG_ANALYZE.RuleListItem[] => {
    return agent
      .get(`/api/${scope}/logs/rules`)
      .query({ scopeID })
      .then((response: any) => response.body);
  };

export const getRule =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (id: string): LOG_ANALYZE.Rule => {
    return agent
      .get(`/api/${scope}/logs/rules/${id}`)
      .query({ scopeID })
      .then((response: any) => response.body);
  };

export const createRule =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (payload: LOG_ANALYZE.Rule) => {
    return agent
      .post(`/api/${scope}/logs/rules`)
      .query({ scopeID })
      .send(payload)
      .then((response: any) => response.body);
  };

export const editRule =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  ({ id, ...payload }: LOG_ANALYZE.Rule) => {
    return agent
      .put(`/api/${scope}/logs/rules/${id}`)
      .query({ scopeID })
      .send(payload)
      .then((response: any) => response.body);
  };

export const toggleRule =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  ({ id, enable }: { id: number; enable: boolean }) => {
    return agent
      .put(`/api/${scope}/logs/rules/${id}/state`)
      .query({ scopeID })
      .send({ enable })
      .then((response: any) => response.body);
  };

export const deleteRule =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (id: number) => {
    return agent
      .delete(`/api/${scope}/logs/rules/${id}`)
      .query({ scopeID })
      .then((response: any) => response.body);
  };

export const testRule =
  ({ scope, scopeID }: LOG_ANALYZE.Scope) =>
  (payload: LOG_ANALYZE.TestRuleQuery): LOG_ANALYZE.TestRuleResp => {
    return agent
      .post(`/api/${scope}/logs/rules/test`)
      .query({ scopeID })
      .send(payload)
      .then((response: any) => response.body);
  };
