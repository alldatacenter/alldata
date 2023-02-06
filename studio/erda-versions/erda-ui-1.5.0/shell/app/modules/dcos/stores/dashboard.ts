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

import { map, isEmpty, some } from 'lodash';
import i18n from 'i18n';
import { multipleGroupDataHandler } from 'common/utils/chart-utils';
import { getClusterList } from 'app/modules/cmp/services/cluster';
import { getAlarmList, IMachineAlarmQuery } from '../services/alarm';
import monitorCommonStore from 'common/stores/monitorCommon';

import { getFilterTypes, getGroupInfos, getNodeLabels, getInstanceList, getChartData } from '../services/dashboard';
import orgStore from 'app/org-home/stores/org';
import { createStore } from 'core/cube';

const RESOURCE_TYPE_MAP = {
  cpu: {
    title: i18n.t('CPU allocation'),
    url: '/api/cluster/resources/containers/group/allocation/cpu',
  },
  mem: {
    title: i18n.t('cmp:Memory allocation'),
    url: '/api/cluster/resources/containers/group/allocation/mem',
  },
  count: {
    title: i18n.t('cmp:operation num'),
    url: '/api/cluster/resources/containers/group/count',
  },
};

interface IState {
  selectedGroups: string[];
  clusterList: ORG_CLUSTER.ICluster[];
  machineList: ORG_MACHINE.IMachine[];
  filterGroup: ORG_DASHBOARD.IFilterType[];
  unGroupInfo: Omit<ORG_DASHBOARD.IGroupInfo, 'groups'> | {};
  groupInfos: ORG_DASHBOARD.IGroupInfo[];
  serviceList: ORG_DASHBOARD.IInstance[];
  jobList: ORG_DASHBOARD.IInstance[];
  instanceList: ORG_DASHBOARD.IInstance[];
  chartList: Array<{
    title: string;
    loading: boolean;
    time?: any[];
    results?: any[];
  }>;
  alarmList: ORG_ALARM.Ticket[];
  alarmPaging: IPaging;
  nodeLabels: ORG_DASHBOARD.INodeLabel[];
}

const initState: IState = {
  selectedGroups: [],
  clusterList: [],
  machineList: [],
  filterGroup: [],
  unGroupInfo: {},
  groupInfos: [],
  serviceList: [],
  jobList: [],
  instanceList: [],
  chartList: [],
  alarmList: [],
  nodeLabels: [],
  alarmPaging: {
    pageNo: 1,
    pageSize: 12,
    total: 0,
  },
};

const appendDisplayNameToGroupInfo = (groupInfos: ORG_DASHBOARD.IGroupInfo[], clusterList: ORG_CLUSTER.ICluster[]) => {
  const nameToDisplayName: Obj<string> = {};
  map(clusterList, (item) => {
    nameToDisplayName[item.name] = item.displayName;
  });
  return groupInfos.map((group) => {
    return {
      ...group,
      displayName: nameToDisplayName[group.name || ''] || group.name,
    };
  });
};

const dashboard = createStore({
  name: 'clusterDashboard',
  state: initState,
  effects: {
    async getFilterTypes({ call, update, select }) {
      const { id: orgId, name: orgName } = orgStore.getState((s) => s.currentOrg);
      const clusterList = await call(getClusterList, { orgId });
      if (isEmpty(clusterList)) return;
      const clusterNameString = map(clusterList, (item) => item.name).join();

      const groupInfos = select((s) => s.groupInfos);
      const newGroupInfos = appendDisplayNameToGroupInfo(groupInfos, clusterList);
      update({ groupInfos: newGroupInfos });
      const filterGroup = await call(getFilterTypes, { clusterName: clusterNameString, orgName });

      update({
        filterGroup,
        clusterList,
        selectedGroups: some(filterGroup, { key: 'cluster' }) ? ['cluster'] : [],
      });
    },
    async getGroupInfos({ call, update, select }, payload: Omit<ORG_DASHBOARD.IGroupInfoQuery, 'orgName'>) {
      const { name: orgName } = orgStore.getState((s) => s.currentOrg);
      const data = await call(getGroupInfos, { orgName, ...payload });
      const { groups: groupInfos, ...unGroupInfo } = data || {};
      const clusterList = select((s) => s.clusterList);
      const newGroupInfos = appendDisplayNameToGroupInfo(groupInfos || [], clusterList);

      update({
        unGroupInfo: unGroupInfo || {},
        groupInfos: newGroupInfos || [],
      });
    },
    async getNodeLabels({ call, update }) {
      let nodeLabels = dashboard.getState((s) => s.nodeLabels);
      if (!nodeLabels.length) {
        nodeLabels = await call(getNodeLabels);
        update({ nodeLabels });
      }
    },
    async getInstanceList(
      { call, update },
      {
        clusters,
        filters,
        instanceType,
        isWithoutOrg,
      }: Merge<ORG_DASHBOARD.IInstanceListQuery, { isWithoutOrg?: boolean }>,
    ) {
      const { name: orgName } = orgStore.getState((s) => s.currentOrg);
      const list = await call(getInstanceList, {
        instanceType,
        orgName: isWithoutOrg ? undefined : orgName,
        clusters,
        filters,
      });
      const instanceMap = {
        service: 'serviceList',
        job: 'jobList',
        all: 'instanceList',
      };
      update({
        [`${instanceMap[instanceType]}`]: list,
      });
    },
    async getChartData({ call }, payload) {
      const { name: orgName } = orgStore.getState((s) => s.currentOrg);
      const { type, ...rest } = payload;
      const timeSpan = monitorCommonStore.getState((s) => s.timeSpan);
      const { startTimeMs, endTimeMs } = timeSpan;
      const query = { start: startTimeMs, end: endTimeMs, orgName };
      const data = await call(getChartData, { url: RESOURCE_TYPE_MAP[type].url, ...rest, query });
      dashboard.reducers.getChartDataSuccess({ data, type, orgName });
    },
    async getAlarmList(
      { call, update },
      payload: Pick<IMachineAlarmQuery, 'endTime' | 'metricID' | 'pageNo' | 'pageSize' | 'startTime'>,
    ) {
      const { id: orgID } = orgStore.getState((s) => s.currentOrg);
      const { list: alarmList, total } = await call(
        getAlarmList,
        {
          ...payload,
          orgID,
          targetID: orgID,
          type: 'machine',
          targetType: 'org',
        },
        { paging: { key: 'alarmPaging', listKey: 'tickets' } },
      );
      update({ alarmList });
      return {
        total,
        list: alarmList,
      };
    },
  },
  reducers: {
    setSelectedGroups(state, selectedGroups) {
      state.selectedGroups = selectedGroups;
    },
    clearClusterList(state) {
      state.clusterList = [];
    },
    getChartDataSuccess(state, { type, data }) {
      const INDEX_MAP = { cpu: 0, mem: 1, count: 2 };
      const { title } = RESOURCE_TYPE_MAP[type];
      const KEYS_MAP = {
        cpu: [
          'group_reduce.{group=tags.addon_id&avg=fields.cpu_allocation&reduce=sum}',
          'group_reduce.{group=tags.service_id&avg=fields.cpu_allocation&reduce=sum}',
          'group_reduce.{group=tags.job_id&avg=fields.cpu_allocation&reduce=sum}',
        ],
        mem: [
          'group_reduce.{group=tags.addon_id&avg=fields.mem_allocation&reduce=sum}',
          'group_reduce.{group=tags.service_id&avg=fields.mem_allocation&reduce=sum}',
          'group_reduce.{group=tags.job_id&avg=fields.mem_allocation&reduce=sum}',
        ],
        count: ['cardinality.tags.addon_id', 'cardinality.tags.job_id', 'cardinality.tags.service_id'],
      };

      const dataHandler = multipleGroupDataHandler(KEYS_MAP[type]);

      state.chartList[INDEX_MAP[type]] = {
        loading: false,
        ...dataHandler(data),
        title,
      };
    },
  },
});

export default dashboard;
