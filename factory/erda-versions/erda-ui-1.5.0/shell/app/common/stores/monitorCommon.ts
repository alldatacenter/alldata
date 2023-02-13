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

import { map, forEach, isFunction } from 'lodash';
import { getTimeSpan, getDefaultPaging, qs } from 'common/utils';
import { getApps } from 'common/services';
import { createStore } from 'core/cube';
import { getModules } from '../services/monitorCommon';
import i18n from 'i18n';
import { ITimeRange, transformRange } from 'common/components/time-select/common';
import moment from 'moment';

const defaultHandler = (data: any) => {
  const modules =
    map(data, (v, k) => {
      return { [k]: v };
    }) || [];
  const reModules = map(modules, (m) => {
    const moduleMap = {} as any;
    forEach(m, (v, k) => {
      moduleMap.label = k;
      moduleMap.value = k;
      moduleMap.children = v.map((item: any, i: number) => {
        return { value: item, label: `${i18n.t('instance')}${i + 1}` };
      });
    });
    return moduleMap;
  });
  return reModules;
};

interface IChartQuery {
  api: string;
  query: object;
  dataHandler: Function | undefined;
  type: string;
}

interface IState {
  timeSpan: ITimeSpan;
  globalTimeSelectSpan: {
    refreshStrategy: string;
    data: ITimeRange;
    range: {
      triggerTime?: number;
    } & ITimeSpan;
  };
  modules: any;
  appGroup: any;
  chosenAppGroup: {
    [pro: string]: string | undefined;
  };
  lastChosenAppGroup: string | undefined;
  subTab: string;
  sortTab: string;
  chosenSortItem: string;
  chosenModule: {
    [pro: string]: string;
  };
  chosenApp: {
    [pro: string]: string;
  };
  projectApps: any[];
  projectAppsPaging: IPaging;
  isShowTraceDetail: boolean;
}
const query = qs.parse(location.search);

let defaultRange: ITimeRange = {
  mode: 'quick',
  quick: 'hours:1',
  customize: {},
};

if (query.mode === 'quick' && query.quick) {
  defaultRange = { mode: 'quick', quick: query.quick, customize: {} };
} else if (query.mode === 'customize' && query.start && query.end) {
  defaultRange = { mode: 'customize', quick: '', customize: { start: moment(+query.start), end: moment(+query.end) } };
}

const { date } = transformRange(defaultRange);

const defaultState: IState = {
  timeSpan: getTimeSpan(),
  globalTimeSelectSpan: {
    refreshStrategy: 'off',
    data: defaultRange,
    range: {
      triggerTime: 0,
      ...getTimeSpan(date),
    },
  },
  modules: {},
  appGroup: {},
  chosenAppGroup: {},
  lastChosenAppGroup: undefined, // 上一次选择的group，有新的appGroup后，匹配上一次选择，匹配后自动选中
  subTab: '',
  sortTab: '',
  chosenSortItem: '',
  chosenModule: {},
  chosenApp: {},
  projectApps: [],
  projectAppsPaging: getDefaultPaging(),
  isShowTraceDetail: false,
};
const monitorCommon = createStore({
  name: 'monitor-common',
  state: defaultState,
  effects: {
    async getModules({ call, getParams }, payload: IChartQuery) {
      const { api, query, dataHandler, type } = payload;
      const { terminusKey } = getParams();
      const data = await call(getModules, { api, query: { ...query, filter_terminus_key: terminusKey } });
      await monitorCommon.reducers.getModulesSuccess({ type, data, dataHandler });
    },
    async getAppGroup({ call }, payload: IChartQuery) {
      const { api, query, dataHandler, type } = payload;
      const data = await call(getModules, { api, query });
      await monitorCommon.reducers.getAppGroupSuccess({ type, data, dataHandler });
    },
    async getProjectApps(
      { call, getParams, select, update },
      payload?: { loadMore?: boolean; q?: string; pageNo?: number },
    ) {
      const { projectId } = getParams();
      const { loadMore = true, pageNo = 1, ...rest } = payload || {};
      const { total, list } = await call(
        getApps,
        { ...rest, pageNo: 1, projectId, mode: 'SERVICE', pageSize: 1000 },
        { paging: { key: 'projectAppsPaging' } },
      ); // 过滤业务应用
      const oldList = select((s) => s.projectApps);
      const newList = loadMore && pageNo !== 1 ? oldList.concat(list) : list;
      update({ projectApps: newList });
      return { list, total };
    },
  },
  reducers: {
    updateState(state, payload) {
      return { ...state, ...payload };
    },
    changeChosenModules(state, payload: { type: string; chosenModule: string }) {
      const { type, chosenModule: curModule } = payload;
      const { chosenModule } = state;
      chosenModule[type] = curModule;
      state.chosenModule = { ...chosenModule };
      state.chosenSortItem = '';
    },
    getProjectAppsSuccess(state, payload: { list: any[]; pageNo: number }) {
      const oldList = state.projectApps;
      const { list, pageNo = 1 } = payload;
      let newList = list as any[];
      if (pageNo !== 1) {
        newList = oldList.concat(list);
      }
      state.projectApps = newList;
    },
    changeChosenApp(state, payload: { chosenApp: { id: string; name: string } }) {
      const { chosenApp = {} } = payload;
      // 修改选中app,则同步取消chosenSortItem;
      state.chosenApp = chosenApp;
      state.appGroup = {};
      state.chosenAppGroup = {};
      state.chosenSortItem = '';
    },
    clearProjectApps(state) {
      state.projectApps = [];
      state.chosenAppGroup = {};
      state.chosenSortItem = '';
    },
    getModulesSuccess(state, payload: { data: any; dataHandler: Function | undefined; type: string }) {
      const { data, dataHandler, type } = payload;
      const { modules } = state;
      modules[type] = isFunction(dataHandler) ? dataHandler(data) : defaultHandler(data);
      state.modules = { ...modules };
    },
    changeChosenAppGroup(state, payload: { type: string; chosenAppGroup: string }) {
      const { type, chosenAppGroup: curChosen } = payload;
      const { chosenAppGroup } = state;
      chosenAppGroup[type] = curChosen;
      const lastChosenAppGroup = curChosen;
      state.chosenAppGroup = { ...chosenAppGroup };
      state.lastChosenAppGroup = lastChosenAppGroup;
      state.chosenSortItem = '';
    },
    clearAppGroup(state, payload) {
      const { type } = payload;
      const { appGroup, chosenAppGroup } = state;
      appGroup[type] = [];
      chosenAppGroup[type] = undefined;
      state.appGroup = { ...appGroup };
      state.chosenAppGroup = { ...chosenAppGroup };
    },
    getAppGroupSuccess(state, payload: { data: any; dataHandler: Function | undefined; type: string }) {
      const { data, dataHandler, type } = payload;
      const { appGroup } = state;
      appGroup[type] = {
        data: isFunction(dataHandler) ? dataHandler(data) : defaultHandler(data),
        loading: false,
      };
      state.appGroup = { ...appGroup };
    },
    clearModules(state, payload: { type: string }) {
      const { type } = payload;
      const { modules, chosenModule } = state;
      modules[type] = [];
      chosenModule[type] = '';
      state.modules = { ...modules };
      state.chosenModule = { ...chosenModule };
    },
    changeTimeSpan(state, payload) {
      state.timeSpan = getTimeSpan(payload);
      state.appGroup = {};
      state.chosenAppGroup = {};
      state.chosenSortItem = '';
    },
    clearTimeSpan(state) {
      state.timeSpan = getTimeSpan();
      state.appGroup = {};
      state.chosenAppGroup = {};
      state.chosenSortItem = '';
    },
    clearMonitorCommon() {
      return { ...defaultState };
    },
    setIsShowTraceDetail(state, payload) {
      state.isShowTraceDetail = payload;
    },
  },
});
export default monitorCommon;
