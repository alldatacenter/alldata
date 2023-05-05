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
import { chartLegendText } from 'charts/text-map.js';
import i18n from 'i18n';

interface IOriginData {
  results: Array<{
    [pro: string]: any;
    name: string;
    key: string;
  }>;
}

export const handlerName = (originData: IOriginData) => {
  const { results } = originData;
  const newResults = results.map(({ name, key, ...others }) => {
    const _name = chartLegendText[`${name}_${key}`] || chartLegendText[name] || name;
    return {
      name: _name || name,
      ...others,
    };
  });
  return { ...originData, results: newResults };
};

export const sortCreator = (moduleName: string, chartName: string, payload?: object) => {
  const commonSort = {
    sortTab: {
      type: 'sortTab',
      tabList: [
        { name: i18n.t('msp:average time'), key: 'time' },
        { name: i18n.t('msp:time percentage'), key: 'percent' },
        { name: i18n.t('msp:throughput'), key: 'cpm' },
      ],
    },
    subTab: {
      type: 'subTab',
      tabList: [
        { name: i18n.t('first paint time'), key: 'wst' },
        { name: i18n.t('first contentful paint time'), key: 'fst' },
        { name: i18n.t('page loading completed'), key: 'pct' },
        { name: i18n.t('msp:resource loading completed'), key: 'rct' },
      ],
    },
    sortList: {
      type: 'sortList',
      chartName: 'sortList',
      moduleName,
      getFetchObj: ({ sortTab, subTab }: { sortTab: string; subTab: string }) => {
        const fetchMap = {
          time: { query: { sortBy: 'time' }, unit: 'ms' },
          percent: { query: { sortBy: 'percent' }, unit: '%' },
          cpm: { query: { sortBy: 'cpm' }, unit: 'cpm' },
        };
        const { query = {}, unit = '' } = fetchMap[sortTab] || {};
        return { extendQuery: { ...query, phase: subTab }, extendHandler: { unit } };
      },
      dataHandler: (originData: object, { extendHandler = {} }) => {
        const resluts = get(originData, 'results') || [];
        const list = resluts || [];
        const reList = map(list, (item) => {
          const { data } = item;
          const { unit = '' } = extendHandler as any;
          return { ...item, value: data[0], unit };
        });
        return { list: reList };
      },
    },
  };
  return { ...commonSort[chartName], ...payload } || null;
};
