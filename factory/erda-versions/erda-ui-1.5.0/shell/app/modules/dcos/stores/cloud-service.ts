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
import {
  addRDS,
  getRDSList,
  getRDSAccountList,
  addRDSAccount,
  resetRDSAccountPWD,
  revokeRDSAccountPriv,
  getRDSDatabaseList,
  addRDSDatabase,
  addMQ,
  getMQList,
  getMQTopicList,
  addMQTopic,
  getMQGroupList,
  addMQGroup,
  addRedis,
  getRedisList,
  getRedisDetails,
  getRDSDetails,
  getMQDetails,
} from '../services/cloud-service';
import i18n from 'i18n';

interface IState {
  RDSList: CLOUD_SERVICE.IRDS[];
  RDSAccountList: CLOUD_SERVICE.IRDSAccountResp[];
  RDSDatabaseList: CLOUD_SERVICE.IRDSDatabaseResp[];
  MQList: CLOUD_SERVICE.IMQ[];
  MQTopicList: CLOUD_SERVICE.IMQTopic[];
  MQGroupList: CLOUD_SERVICE.IMQGroup[];
  redisList: CLOUD_SERVICE.IRedis[];
  RDSDetails: CLOUD_SERVICE.IDetailsResp[];
  MQDetails: CLOUD_SERVICE.IDetailsResp[];
  redisDetails: CLOUD_SERVICE.IDetailsResp[];
}

const initState: IState = {
  RDSList: [],
  RDSDetails: [],
  RDSAccountList: [],
  RDSDatabaseList: [],
  MQList: [],
  MQTopicList: [],
  MQGroupList: [],
  MQDetails: [],
  redisList: [],
  redisDetails: [],
};

const cloudService = createStore({
  name: 'cloud-service',
  state: initState,
  effects: {
    async getRDSList({ call, update }, payload: CLOUD_SERVICE.ICloudServiceQuery) {
      const { list: RDSList = [] } = await call(getRDSList, payload);
      update({ RDSList });
    },
    async addRDS({ call }, payload: CLOUD_SERVICE.IRDSCreateBody) {
      const res = await call(addRDS, payload, { fullResult: true });
      return res;
    },
    async getRDSDetails({ call, update }, payload: CLOUD_SERVICE.IDetailsQuery) {
      const RDSDetails = await call(getRDSDetails, payload);
      update({ RDSDetails });
    },
    async getRDSAccountList({ call, update }, payload: CLOUD_SERVICE.IUserQuery) {
      const { list: RDSAccountList = [] } = await call(getRDSAccountList, payload);
      update({ RDSAccountList });
    },
    async addRDSAccount({ call }, payload: CLOUD_SERVICE.IRDSCreateAccountBody) {
      const res = await call(addRDSAccount, payload);
      return res;
    },
    async resetRDSAccountPWD({ call }, payload: CLOUD_SERVICE.IRDSAccountBody) {
      const res = await call(resetRDSAccountPWD, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    async revokeRDSAccountPriv({ call }, payload: CLOUD_SERVICE.IRDSRevokeAccountPrivBody) {
      const res = await call(revokeRDSAccountPriv, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    async getRDSDatabaseList({ call, update }, payload: CLOUD_SERVICE.IDatabaseQuery) {
      const { list: RDSDatabaseList = [] } = await call(getRDSDatabaseList, payload);
      update({ RDSDatabaseList });
    },
    async addRDSDatabase({ call }, payload: CLOUD_SERVICE.IRDSDatabaseBody) {
      const res = await call(addRDSDatabase, payload);
      return res;
    },
    async getMQList({ call, update }, payload: CLOUD_SERVICE.ICloudServiceQuery) {
      const { list: MQList = [] } = await call(getMQList, payload);
      update({ MQList });
    },
    async addMQ({ call }, payload: CLOUD_SERVICE.IMQCreateBody) {
      const res = await call(addMQ, payload, { fullResult: true });
      return res;
    },
    async getMQDetails({ call, update }, payload: CLOUD_SERVICE.IDetailsQuery) {
      const MQDetails = await call(getMQDetails, payload);
      update({ MQDetails });
    },
    async getMQTopicList({ call, update }, payload: CLOUD_SERVICE.IMQManageQuery) {
      const { list: MQTopicList = [] } = await call(getMQTopicList, payload);
      update({ MQTopicList });
    },
    async addMQTopic({ call }, payload: CLOUD_SERVICE.IMQTopicBody) {
      const res = await call(addMQTopic, payload);
      return res;
    },
    async getMQGroupList({ call, update }, payload: CLOUD_SERVICE.IMQGroupQuery) {
      const { list: MQGroupList = [] } = await call(getMQGroupList, payload);
      update({ MQGroupList });
    },
    async addMQGroup({ call }, payload: CLOUD_SERVICE.IMQGroupBody) {
      const res = await call(addMQGroup, payload);
      return res;
    },
    async getRedisList({ call, update }, payload: CLOUD_SERVICE.ICloudServiceQuery) {
      const { list: redisList = [] } = await call(getRedisList, payload);
      update({ redisList });
    },
    async addRedis({ call }, payload: CLOUD_SERVICE.IRedisCreateBody) {
      const res = await call(addRedis, payload, { fullResult: true });
      return res;
    },
    async getRedisDetails({ call, update }, payload: CLOUD_SERVICE.IDetailsQuery) {
      const redisDetails = await call(getRedisDetails, payload);
      update({ redisDetails });
    },
  },
  reducers: {
    clearRDSDetails(state) {
      state.RDSDetails = [];
    },
    clearRDSAccountList(state) {
      state.RDSAccountList = [];
    },
    clearRDSDatabaseList(state) {
      state.RDSDatabaseList = [];
    },
    clearMQDetails(state) {
      state.MQDetails = [];
    },
    clearMQTopicList(state) {
      state.MQTopicList = [];
    },
    clearMQGroupList(state) {
      state.MQGroupList = [];
    },
    clearRedisDetails(state) {
      state.redisDetails = [];
    },
  },
});

export default cloudService;
