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
import i18n from 'i18n';

interface IQuery {
  [pro: string]: any;
  range: string;
}
const handleApdex = (originData: object, { chartName, query }: { chartName: string; query: IQuery }) => {
  const xAxis: any[] = [];
  let results: any[] = [];
  const dataKey = `range.${query.range}`;
  const { data = null } = get(originData, `results[0].data[0]['${dataKey}']`) || {};
  if (data) {
    results = map(data, (item, i) => {
      const { count, percent, min, max } = item;
      let name = '';
      if (chartName === 'apdex') {
        name = [i18n.t('msp:satisfied'), i18n.t('msp:tolerable'), i18n.t('msp:not satisfied')][i];
      } else {
        name = `[${min},${max}]`;
      }
      xAxis.push(name);
      return { name, value: count, label: `${percent.toFixed(2)} %` };
    });
  }
  return { results: [{ data: results, chartType: 'bar' }], xAxis };
};

export const ApiMap = {
  apdex: {
    fetchApi: 'ta_apdex/range',
    query: { range: 'plt', ranges: '0:2000,2000:8000,8000:', source: true },
    dataHandler: handleApdex,
  },
  timing: {
    dataHandler: handleApdex,
  },
  dimension: {
    query: { limit: 10, sort: 'countPercent_plt', countPercent: 'plt' },
    dataHandler: (originData: object, { chartName }: { chartName: string }) => {
      const pieArr: any[] = [];
      const data = get(originData, 'results[0].data');
      if (data) {
        map(data, (item) => {
          const { tag = '', name = '', ...rest } = item['countPercent.plt'] || {};
          pieArr.push({ name: tag || name, value: rest.data });
        });
      }
      let results: any[] = [];
      if (pieArr.length) {
        results = [{ name: chartName, data: pieArr }];
      }
      return { results };
    },
  },
};
