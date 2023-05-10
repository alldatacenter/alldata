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

import { get, map } from 'lodash';
import { createStore } from 'core/cube';
import { loadComparative } from '../services/comparative';
import i18n from 'i18n';

interface ILoadQuery {
  query: {
    [pro: string]: any;
    type: string;
  };
}

const initState = {
  comparative: {},
};

const comparativeStore = createStore({
  name: 'BIComparative',
  state: initState,
  effects: {
    async loadComparative({ call }, payload: ILoadQuery) {
      const { query } = payload;
      const { type, ...rest } = query;
      const result = await call(loadComparative, rest);
      comparativeStore.reducers.loadComparativeSuccess({ result, type, query: rest });
    },
  },
  reducers: {
    loadComparativeSuccess(state, payload) {
      const { result, type, query } = payload;
      const dataKey = `range.${query.range}`;
      const list = get(result, 'results[0].data');
      let reData = [] as any[];
      if (list) {
        reData = map(list, (l) => {
          const xAxis: string[] = [];
          let results = [];
          let titleText = '';
          const curData = get(l, `['${dataKey}']`);

          titleText = curData.tag || curData.name;
          results = map(curData.data, (item, i) => {
            const { count, percent, min, max } = item;
            let name = '';
            if (type === 'apdex') {
              name = [i18n.t('msp:satisfied'), i18n.t('msp:tolerable'), i18n.t('msp:not satisfied')][i];
            } else {
              name = `[${min},${max}]`;
            }
            xAxis.push(name);
            return { name, value: count, label: `${percent.toFixed(2)} %` };
          });
          return { results: [{ data: results, chartType: 'bar' }], xAxis, titleText };
        });
      }
      const comparative = reData;
      state.comparative = comparative;
    },
  },
});

export default comparativeStore;
