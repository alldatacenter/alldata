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
import { getAggregation, getFields, getLogAnalytics, getLogAnalyticContext } from 'msp/services/log-analytics';
import { produce } from 'immer';

export type IMenu = LOG_ANALYTICS.IField & {
  subMenu?: LOG_ANALYTICS.IAggregationBuckets[];
  total?: LOG_ANALYTICS.IAggregation['total'];
};

interface IState {
  fields: LOG_ANALYTICS.IField[];
  menu: IMenu[];
  showTags: string[];
  logList: LOG_ANALYTICS.LogItem[];
  logTotal: number;
}

const initState: IState = {
  fields: [],
  menu: [],
  showTags: [],
  logList: [],
  logTotal: 0,
};

export const localFieldsKey = 'map-local-fields';

const mspLogAnalyticsStore = createStore({
  name: 'mapLogAnalytics',
  state: initState,
  effects: {
    async getFields({ call, update, getParams }) {
      const { addonId } = getParams();
      let fields = await call(getFields, { addon: addonId });
      const localFieldsStr = window.localStorage.getItem(localFieldsKey);
      let localFields = fields;
      if (localFieldsStr) {
        localFields = JSON.parse(localFieldsStr);
        if (Array.isArray(localFields) && localFields.length === fields.length) {
          fields = localFields;
        }
      }
      window.localStorage.setItem(localFieldsKey, JSON.stringify(fields));
      update({
        fields,
        menu: fields.filter((t) => t.supportAggregation),
        showTags: fields.filter((item) => item.display).map((item) => item.fieldName),
      });
    },
    async getLogAnalytics({ call, update, getParams }, payload: Omit<LOG_ANALYTICS.QuerySearch, 'addon'>) {
      const { addonId } = getParams();
      const { data, total } = await call(
        getLogAnalytics,
        { addon: addonId, ...payload },
        { paging: { key: 'mspLogAnalyticsList' } },
      );
      update({
        logList: data ?? [],
        logTotal: total,
      });
    },
    async getLogAnalyticContext({ call, getParams }, payload: Omit<LOG_ANALYTICS.QueryContext, 'addon'>) {
      const { addonId } = getParams();
      const props = await call(getLogAnalyticContext, { addon: addonId, ...payload });
      const { data } = props;
      return data ?? [];
    },
    async getAggregation(
      { call, update, getParams, select },
      { targetKey, ...payload }: Omit<LOG_ANALYTICS.QueryAggregation, 'addon'> & { targetKey: string },
    ) {
      const { addonId } = getParams();
      const menu = select((s) => s.menu);
      const { aggFields, total } = await call(getAggregation, { addon: addonId, ...payload });
      const newMenu = produce(menu, (draft) => {
        const currentMenu = draft.find((t) => t.fieldName === targetKey)!;
        currentMenu.subMenu = aggFields[targetKey].buckets;
        currentMenu.total = total;
      });
      update({ menu: newMenu });
    },
  },
  reducers: {
    updateFields(state, fields) {
      state.fields = fields;
    },
    updateShowTags(state, fields: LOG_ANALYTICS.IField[]) {
      window.localStorage.setItem(localFieldsKey, JSON.stringify(fields));
      state.fields = fields;
      state.showTags = fields.filter((item) => item.display).map((item) => item.fieldName);
    },
  },
});

export default mspLogAnalyticsStore;
