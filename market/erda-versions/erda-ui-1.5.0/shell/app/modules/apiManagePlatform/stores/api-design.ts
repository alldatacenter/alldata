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

import { createFlatStore } from 'core/cube';
import { API_FORM_KEY, API_WS_MSG_TYPE, API_PROPERTY_REQUIRED } from 'app/modules/apiManagePlatform/configs';
import {
  getTreeList,
  getApiDetail,
  deleteTreeNode,
  renameTreeNode,
  moveTreeNode,
  copyTreeNode,
  createTreeNode,
  publishApi,
  getApiAssets,
  getSchemaParams,
  validateApiSwagger,
} from '../services/api-design';
import { isEmpty, map, keys } from 'lodash';
import i18n from 'i18n';
import { message } from 'antd';

interface IState {
  resourceList: API_SETTING.IApiResource[];
  dataTypeList: API_SETTING.IDataType[];
  openApiDoc: Obj;
  branchList: API_SETTING.IFileTree[];
  apiDetail: API_SETTING.IApiDetail;
  wsQuery: Obj;
  apiWs: any;
  apiLockState: boolean;
  isApiReadOnly: boolean;
  lockUser: string;
  apiAssets: API_SETTING.IApiAsset[];

  schemaParams: Obj[];
  isDocChanged: boolean;
  isApiDocError: boolean;

  formErrorNum: number;
  docValidData: {
    valid: boolean;
    msg: string;
  };
  isSaved: boolean;
}

const initState: IState = {
  apiDetail: {} as API_SETTING.IApiDetail,
  resourceList: [],
  dataTypeList: [],
  openApiDoc: {},
  branchList: [],
  wsQuery: {},
  apiWs: null,
  apiLockState: true,
  isApiReadOnly: false,
  apiAssets: [],
  schemaParams: [],
  isDocChanged: false,
  isApiDocError: false,
  formErrorNum: 0,
  lockUser: '',
  docValidData: {
    valid: true,
    msg: '',
  },
  isSaved: false,
};

const apiDesignStore = createFlatStore({
  name: 'apiDesign',
  state: initState,
  effects: {
    async getTreeList({ call, update }, payload: API_SETTING.ITreeListQuery) {
      const list = await call(getTreeList, payload);
      if (payload.scope) {
        update({ branchList: list });
      }
      return list;
    },
    async getApiDetail({ call, update }, payload: string) {
      const data = (await call(getApiDetail, payload)) || {};
      const worker = new Worker('/static/api-design-worker.js');
      worker.postMessage(data);

      const res = new Promise<API_SETTING.IApiDocDetail>((resolve) => {
        worker.onmessage = function (event) {
          const openApiDoc = typeof event.data === 'object' && event.data !== null ? event.data : {};
          const isReadOnly = data.meta?.readOnly || false;

          update({
            openApiDoc,
            isDocChanged: false,
            apiLockState: true,
            isApiReadOnly: isReadOnly,
            docValidData: {
              valid: data.meta?.valid || false,
              msg: data.meta?.error || '',
            },
          });

          resolve({
            openApiDoc,
            name: data.name,
            asset: data.meta?.asset,
            readOnly: isReadOnly,
          });
        };
      });
      return res;
    },
    async copyTreeNode({ call }, payload: { pinode: string; inode: string }) {
      const data = await call(copyTreeNode, payload);
      return data;
    },
    async deleteTreeNode({ call }, payload: { inode: string }) {
      const data = await call(deleteTreeNode, payload, {
        successMsg: i18n.t('{action} successfully', { action: i18n.t('delete') }),
      });
      return data;
    },
    async createTreeNode({ call }, payload: { pinode: string; name: string }) {
      const data = await call(createTreeNode, payload, {
        successMsg: i18n.t('{action} successfully', { action: i18n.t('add') }),
      });
      return data;
    },
    async renameTreeNode({ call }, payload: { name: string; inode: string }) {
      const data = await call(renameTreeNode, payload, {
        successMsg: i18n.t('{action} successfully', { action: i18n.t('dop:rename') }),
      });
      return data;
    },
    async moveTreeNode({ call }, payload: { pinode: string; inode: string }) {
      const data = await call(moveTreeNode, payload, {
        successMsg: i18n.t('{action} successfully', { action: i18n.t('dop:move') }),
      });
      return data;
    },
    async commitSaveApi({ select }) {
      message.info(i18n.t('dop:It is being saved and it will take effect later.'));

      const validData = await apiDesignStore.validateApiSwagger();
      if (!validData.valid) {
        message.warning(validData.msg);
      } else {
        const { wsQuery, apiWs, openApiDoc } = select((s) => s);
        const wsCommand = {
          ...wsQuery,
          type: API_WS_MSG_TYPE.commit,
          data: {
            ...wsQuery.data,
            content: JSON.stringify(openApiDoc),
          },
        };
        apiWs.send(JSON.stringify(wsCommand));
      }
    },
    async autoSaveApi({ select }) {
      const { wsQuery, apiWs, openApiDoc, formErrorNum, isDocChanged, docValidData } = select((s) => s);

      if (formErrorNum > 0) {
        return;
      } else if (isDocChanged) {
        const validData = await apiDesignStore.validateApiSwagger();
        if (!validData.valid) return;
      } else if (!docValidData.valid) {
        return;
      }

      const wsCommand = {
        ...wsQuery,
        type: API_WS_MSG_TYPE.save,
        data: {
          ...wsQuery.data,
          content: JSON.stringify(openApiDoc),
        },
      };
      apiWs.send(JSON.stringify(wsCommand));

      return true;
    },
    async publishApi({ call }, payload: API_SETTING.IPublishAPi) {
      const data = await call(publishApi, payload, {
        successMsg: i18n.t('{action} successfully', { action: i18n.t('publisher:release') }),
      });
      return data;
    },
    async getApiAssets({ call, update }, payload: API_SETTING.IApiAssetsQuery) {
      const { list } = await call(getApiAssets, payload);
      const apiAssets = map(list, (item) => item.asset);
      update({ apiAssets });
      return list;
    },
    async getSchemaParams({ call, update }, payload: { inode: string }) {
      const data = await call(getSchemaParams, payload);
      const params = data?.meta?.schemas || {};
      const list: Obj[] = [];
      map(keys(params), (p) => {
        const propertyItem = { ...params[p], tableName: p };
        const { properties } = propertyItem;
        const propertyList = map(keys(properties), (propertyName) => {
          const propertyData = { ...properties[propertyName] };
          propertyData[API_FORM_KEY] = propertyName;
          propertyData[API_PROPERTY_REQUIRED] = propertyItem?.required?.includes(propertyName) || false;
          return propertyData;
        });
        propertyItem.list = propertyList;
        list.push(propertyItem);
      });
      update({ schemaParams: list });
      return list;
    },
    async validateApiSwagger({ call, select, update }) {
      const { openApiDoc } = select((s) => s);
      const data = await call(validateApiSwagger, { content: JSON.stringify(openApiDoc) });

      const docValidData = {
        valid: data.success || false,
        msg: data?.err || '',
      };
      update({ docValidData });
      return docValidData;
    },
  },
  reducers: {
    updateOpenApiDoc(state, detail) {
      state.openApiDoc = detail;
      if (!isEmpty(detail)) {
        state.isDocChanged = true;
      }
    },
    updateWsQuery(state, query) {
      state.wsQuery = query;
    },
    updateApiWs(state, ws) {
      state.apiWs = ws;
    },
    updateApiLockState(state, isLocked) {
      if (state.apiLockState !== isLocked) {
        state.apiLockState = isLocked;
      }
    },
    updateLockUser(state, nickName) {
      state.lockUser = nickName;
    },
    updateFormErrorNum(state, errorNum) {
      state.formErrorNum = errorNum;
    },
    resetDocValidData(state) {
      state.docValidData = {
        valid: true,
        msg: '',
      };
    },
    setDocChangedState(state, isChanged) {
      state.isDocChanged = isChanged;
    },
    setSavedState(state, isSaved) {
      state.isSaved = isSaved;
    },
    clearSchemaParams(state) {
      state.schemaParams = [];
    },
  },
});

export default apiDesignStore;
