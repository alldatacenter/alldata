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

export const ApiMap = {
  sortList: {
    fetchApi: 'httpException/count',
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const fetchMap = {
        code: { query: { sortBy: 'code' }, unit: i18n.t('times') },
      };
      const { query = {}, unit = '' } = fetchMap[sortTab] || {};
      return { extendQuery: { ...query }, extendHandler: { unit } };
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
  exception: {
    fetchApi: 'httpException/list',
  },
  slowTrack: {
    fetchApi: 'httpException/trace',
  },
};
