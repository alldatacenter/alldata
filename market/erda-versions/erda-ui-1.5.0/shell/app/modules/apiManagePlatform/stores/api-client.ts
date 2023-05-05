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
  updateClient,
  createClient,
  getClientList,
  createContract,
  deleteClient,
  getClientDetail,
  getContractDetail,
  getContractList,
} from '../services/api-client';
import { getDefaultPaging } from 'common/utils';
import i18n from 'i18n';
import breadcrumbStore from 'layout/stores/breadcrumb';

interface IState {
  clientList: API_CLIENT.ClientItem[];
  clientListPaging: IPaging;
  clientDetail: API_CLIENT.ClientItem;
  contractDetail: API_CLIENT.ContractItem;
  provingContractList: API_CLIENT.Contract[];
  provingContractPaging: IPaging;
  provedContractList: API_CLIENT.Contract[];
  provedContractPaging: IPaging;
  disprovedContractList: API_CLIENT.Contract[];
  disprovedContractPaging: IPaging;
  unprovedContractList: API_CLIENT.Contract[];
  unprovedContractPaging: IPaging;
}

const initState: IState = {
  clientList: [],
  clientListPaging: getDefaultPaging(),
  clientDetail: {} as API_CLIENT.ClientItem,
  contractDetail: {} as API_CLIENT.ContractItem,
  provingContractList: [],
  provingContractPaging: getDefaultPaging(),
  provedContractList: [],
  provedContractPaging: getDefaultPaging(),
  disprovedContractList: [],
  disprovedContractPaging: getDefaultPaging(),
  unprovedContractList: [],
  unprovedContractPaging: getDefaultPaging(),
};

const apiClient = createStore({
  name: 'apiClient',
  state: initState,
  effects: {
    async getClientList({ call, update }, payload: API_CLIENT.QueryClient) {
      const { list } = await call(getClientList, payload, { paging: { key: 'clientListPaging' } });
      update({ clientList: list });
      return list;
    },
    async createClient({ call }, payload: API_CLIENT.CreateClient) {
      const res = await call(createClient, payload);
      return res;
    },
    async updateClient({ call }, payload: API_CLIENT.UpdateClient) {
      const { resetClientSecret } = payload;
      const successMsg = i18n.t('{action} successfully', {
        action: resetClientSecret ? i18n.t('reset') : i18n.t('update'),
      });
      const res = await call(updateClient, payload, { successMsg });
      return res;
    },
    async getClientDetail({ call, update }, payload: API_CLIENT.Common) {
      const clientDetail = await call(getClientDetail, payload);
      breadcrumbStore.reducers.setInfo('clientName', clientDetail.client.displayName);
      update({ clientDetail });
      return clientDetail;
    },
    async deleteClient({ call }, payload: API_CLIENT.Common) {
      const res = await call(deleteClient, payload, {
        successMsg: i18n.t('{action} successfully', { action: i18n.t('delete') }),
      });
      return res;
    },
    async createContract({ call }, payload: API_CLIENT.CreteContract) {
      const res = await call(createContract, payload);
      return res;
    },
    async getContractList({ call, update }, payload: API_CLIENT.QueryContractList) {
      const pagingKey = `${payload.status}ContractPaging`;
      const listKey = `${payload.status}ContractList`;
      const res = await call(getContractList, payload, { paging: { key: pagingKey } });
      update({ [listKey]: res.list || [] });
      return res;
    },
    async getContractDetail({ call, update }, payload: API_CLIENT.QueryContract) {
      const contractDetail = await call(getContractDetail, payload);
      update({ contractDetail });
      return contractDetail;
    },
  },
  reducers: {
    clearClientDetail(state) {
      state.clientDetail = {} as API_CLIENT.ClientItem;
    },
    clearContractList(state) {
      state.provedContractList = [];
      state.provingContractList = [];
      state.disprovedContractList = [];
      state.unprovedContractList = [];
      state.provedContractPaging = getDefaultPaging();
      state.provingContractPaging = getDefaultPaging();
      state.disprovedContractPaging = getDefaultPaging();
      state.unprovedContractPaging = getDefaultPaging();
    },
  },
});

export default apiClient;
