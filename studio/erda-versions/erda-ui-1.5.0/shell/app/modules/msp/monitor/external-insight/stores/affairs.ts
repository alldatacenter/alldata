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

import { get } from 'lodash';
import { createStore } from 'core/cube';
import { getSubSlowHttpList, getSubErrorHttpList } from '../services';

interface IState {
  subSlowHttpList: Obj<MONITOR_EI.ISubTableData[]>;
  subErrorHttpList: Obj<MONITOR_EI.ISubTableData[]>;
}

const initState: IState = {
  subSlowHttpList: {},
  subErrorHttpList: {},
};

const Affairs = createStore({
  name: 'EIAffairs',
  state: initState,
  effects: {
    async getSubSlowHttpList({ call, update, select }, payload: IChartQuery) {
      const subSlowHttpList = select((s) => s.subSlowHttpList);
      const { filter_source_service_name } = payload;
      const data = await call(getSubSlowHttpList, payload);
      update({
        subSlowHttpList: { ...subSlowHttpList, [filter_source_service_name]: get(data, 'results[0].data') || [] },
      });
    },
    async getSubErrorHttpList({ call, update, select }, payload: IChartQuery) {
      const subErrorHttpList = select((s) => s.subErrorHttpList);
      const { filter_source_service_name, filter_http_status_code } = payload;
      const errorKey = `${filter_source_service_name}_${filter_http_status_code}`;
      const data = await call(getSubErrorHttpList, payload);
      update({ subErrorHttpList: { ...subErrorHttpList, [errorKey]: get(data, 'results[0].data') || [] } });
    },
  },
});
export default Affairs;
