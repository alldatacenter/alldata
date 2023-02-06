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

import { getDefaultPaging } from 'common/utils';
import { createFlatStore } from 'core/cube';
import {
  getServiceInsList,
  getProfileList,
  startProfile,
  stopProfile,
  getProfileStatus,
  getJVMInfo,
} from '../services/jvm';

interface OptionItem {
  label: string;
  value: string;
  children?: OptionItem[];
}

export enum ProfileStateMap {
  PENDING = 'pending',
  RUNNING = 'running',
  COMPLETED = 'completed',
  FAILED = 'failed',
  TERMINATING = 'terminating',
}

interface IState {
  services: OptionItem[];
  runningList: JVM.ProfileItem[];
  runningPaging: IPaging;
  historyList: JVM.ProfileItem[];
  historyPaging: IPaging;
  jvmInfo: { [k: string]: Array<{ key: string; value: string }> };
}

const initState: IState = {
  services: [],
  runningList: [],
  runningPaging: getDefaultPaging(),
  historyList: [],
  historyPaging: getDefaultPaging(),
  jvmInfo: {},
};

const jvmStore = createFlatStore({
  name: 'jvm',
  state: initState,
  effects: {
    async getServiceInsList({ call, update }, insId: string) {
      const res = await call(getServiceInsList, insId);
      const services = res.map((a) => ({
        label: a.applicationName,
        value: a.applicationId,
        children: a.services.map((s) => ({
          label: s.serviceName,
          value: s.serviceId,
          children: s.instances.map((i) => ({
            label: i.instanceName,
            value: i.instanceId,
          })),
        })),
      }));
      update({ services });
    },
    async getProfileList({ call, update }, data: Merge<JVM.ProfileListQuery, { isHistory: boolean }>) {
      const { isHistory, ...rest } = data;
      const { list } = await call(getProfileList, rest, {
        paging: { key: isHistory ? 'historyPaging' : 'runningPaging' },
      });
      if (isHistory) {
        update({ historyList: list });
      } else {
        update({ runningList: list });
      }
    },
    async startProfile({ call }, data: JVM.StartProfileBody) {
      return call(startProfile, data);
    },
    async stopProfile({ call }, payload: JVM.ProfileStatusQuery) {
      return call(stopProfile, payload);
    },
    async getProfileStatus({ call }, payload: JVM.ProfileStatusQuery) {
      return call(getProfileStatus, payload);
    },
    async getJVMInfo({ select, call, update }, payload: JVM.JVMInfoQuery) {
      const { list } = await call(getJVMInfo, payload);
      const preJvmInfo = select((s) => s.jvmInfo);
      update({
        jvmInfo: {
          ...preJvmInfo,
          [payload.scope]: list,
        },
      });
    },
  },
});

export default jvmStore;
