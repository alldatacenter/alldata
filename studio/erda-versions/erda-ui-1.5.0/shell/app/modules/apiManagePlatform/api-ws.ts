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

import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { API_WS_MSG_TYPE } from 'app/modules/apiManagePlatform/configs';
import { message, notification } from 'antd';
import { get } from 'lodash';
import i18n from 'i18n';
import { setApiWithOrg } from 'common/utils';

export const API_WS_URL = setApiWithOrg('/api/apim-ws/api-docs/filetree');

interface IInitApiWs {
  pinode: string;
  inode: string;
}

export const initApiWs = (payload: IInitApiWs) => {
  const {
    updateWsQuery,
    updateApiWs,
    updateApiLockState,
    autoSaveApi,
    updateLockUser,
    setDocChangedState,
    setSavedState,
  } = apiDesignStore;

  const { pinode, inode } = payload;
  let heartBeatTimer: any = null;
  let autoSaveTimer: any = null;
  let timer: any = null;
  if ('WebSocket' in window) {
    // eslint-disable-next-line no-console
    console.log('您的浏览器支持 WebSocket!---', 'API Setting');

    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    const ws = new WebSocket(`${protocol}${window.location.host}${API_WS_URL}/${inode}`);
    updateApiWs(ws);
    const wsQuery = {
      sessionID: '',
      messageID: 0,
      createdAt: '',
      data: {
        pinode,
        inode,
      },
    };
    ws.onopen = function () {
      heartBeatTimer = setInterval(() => {
        ws.send(JSON.stringify({ ...wsQuery, type: API_WS_MSG_TYPE.beat }));
      }, 10 * 1000);
      autoSaveTimer = setInterval(autoSaveApi, 30 * 1000);
    };
    ws.onmessage = (evt) => {
      const data = evt?.data ? JSON.parse(evt.data) : {};

      timer && clearTimeout(timer);

      if (data.type === 'commit_response') {
        timer = setTimeout(() => {
          message.success(i18n.t('{action} successfully', { action: i18n.t('save') }));
          setDocChangedState(false);
          setSavedState(true);
        }, 1000);
      } else if (data.type === 'auto_save_response') {
        timer = setTimeout(() => {
          message.success(i18n.t('{action} successfully', { action: i18n.t('dop:auto save') }));
          setDocChangedState(false);
        }, 1000);
      }

      if (!wsQuery?.sessionID) {
        wsQuery.sessionID = data.sessionID;
        wsQuery.createdAt = data.createdAt;
        updateWsQuery(wsQuery);
      }
      if (data.type !== 'error_response') {
        if (get(data, 'data.meta.lock.locked')) {
          updateApiLockState(true);
          updateLockUser(data.data.meta.lock.nickName);
        } else {
          updateApiLockState(false);
        }
      }
    };

    ws.onclose = function () {
      // eslint-disable-next-line no-console
      console.log('连接已关闭...');
      updateApiWs(null);
      updateApiLockState(true);
      updateWsQuery({});
      clearInterval(heartBeatTimer);
      clearInterval(autoSaveTimer);
    };
  } else {
    notification.error({
      message: i18n.t('dop:connection failed'),
      description: i18n.t('dop:your browser does not support WebSocket!'),
    });
  }
};
