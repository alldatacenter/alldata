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
import { map, isFunction } from 'lodash';
import routeInfoStore from 'core/stores/route';
import { getMetaGroups, getMetaData } from '../services/monitor-common-metadata';

export interface IState {
  metaGroups: any[];
  metaConstantMap: MONITOR_COMMON_METADATA.MetaConstantMap;
  metaMetrics: MONITOR_COMMON_METADATA.MetaMetrics;
}

export enum MonitorMetaDataScope {
  ORG = 'org',
  MICRO_SERVICE = 'micro_service',
}

export enum MonitorMetaDataMode {
  QUERY = 'query',
  ANALYSIS = 'analysis',
}

export const createMonitorMetaDataStore = (
  scope: MonitorMetaDataScope,
  mode: MonitorMetaDataMode,
  scopeId?: string | (() => string),
) => {
  const initState: IState = {
    metaGroups: [],
    metaConstantMap: { types: {}, filters: [] } as MONITOR_COMMON_METADATA.MetaConstantMap,
    metaMetrics: [],
  };
  // 大盘和告警指标数据未统一的特殊处理，后续统一后可去掉
  const isQueryMode = mode === MonitorMetaDataMode.QUERY;

  const monitorMetaDataStore = createStore({
    name: `${scope}${mode}MonitorMetaData`,
    state: initState,
    effects: {
      async getMetaGroups({ call }) {
        const { terminusKey } = routeInfoStore.getState((s) => s.params);
        const _scopeId = isFunction(scopeId) ? scopeId() : scopeId;
        const groups = await call(getMetaGroups, {
          scope,
          scopeId: _scopeId || terminusKey,
          mode,
          format: isQueryMode ? 'influx' : undefined,
        });

        monitorMetaDataStore.reducers.convertGroups(groups);
      },
      async getMetaData({ call, update }, { groupId }: { groupId: string }) {
        const { terminusKey } = routeInfoStore.getState((s) => s.params);
        const _scopeId = isFunction(scopeId) ? scopeId() : scopeId;
        const metaData = await call(getMetaData, {
          scope,
          scopeId: _scopeId || terminusKey,
          mode,
          groupId,
          format: isQueryMode ? 'influx' : undefined,
          appendTags: isQueryMode ? 'true' : undefined,
        });
        const { meta, metrics } = metaData;

        update({
          metaConstantMap: meta,
          metaMetrics: metrics,
        });
        return metrics;
      },
    },
    reducers: {
      convertGroups(state, groups: any[]) {
        const convertGroups: any = (data: any[]) =>
          map(data, ({ id, name, children }) => {
            if (children) {
              return { value: id, label: name, children: convertGroups(children) };
            } else {
              return { value: id, label: name };
            }
          });
        state.metaGroups = convertGroups(groups);
      },
    },
  });

  return monitorMetaDataStore;
};
