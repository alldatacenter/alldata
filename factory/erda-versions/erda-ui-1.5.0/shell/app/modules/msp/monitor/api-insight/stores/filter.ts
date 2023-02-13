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

interface IState {
  searchFields: {
    filter_api: undefined | string;
    filter_mthd: undefined | string;
    filter_hts: undefined | string;
    filter_upfs: undefined | string;
    filter_dsrv: undefined | string;
    filter_dapp: undefined | string;
  };
}

const initState: IState = {
  searchFields: {
    filter_api: undefined,
    filter_mthd: undefined,
    filter_hts: undefined,
    filter_upfs: undefined,
    filter_dsrv: undefined,
    filter_dapp: undefined,
  },
};

const filter = createStore({
  name: 'apiMonitorFilter',
  state: initState,
  effects: {
    async getSearchFields({ select }) {
      const searchFields = select((s) => s.searchFields);
      return searchFields;
    },
  },
  reducers: {
    updateSearchFields(state, payload) {
      state.searchFields = { ...state.searchFields, ...payload };
    },
    resetSearchFields(state) {
      state.searchFields = {} as any;
    },
  },
});

export default filter;
