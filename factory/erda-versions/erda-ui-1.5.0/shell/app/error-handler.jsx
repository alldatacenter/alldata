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

import React from 'react';
import { notification } from 'antd';
import userStore from './user/stores';

// use static data instead of i18n match
const statusMap = {
  400: {
    level: 'warning',
    zhMsg: '请求参数错误，请检查后再试',
    enMsg: 'Request parameter is wrong, please check and try again',
  },
  401: { level: 'warning', zhMsg: '您当前未登录', enMsg: 'You are not currently logged in' },
  403: {
    level: 'warning',
    zhMsg: '很抱歉，您暂无权限进行此操作',
    enMsg: 'Sorry, you do not have permission to perform this operation',
  },
  500: {
    level: 'error',
    zhMsg: '很抱歉，服务出现问题，我们将尽快修复',
    enMsg: 'Sorry, there is a problem with the service, we will fix it as soon as possible',
  },
  503: {
    level: 'error',
    zhMsg: '很抱歉，服务暂时不可用，请稍后再试',
    enMsg: 'Sorry, the service is temporarily unavailable, please try again later',
  },
  504: {
    level: 'error',
    zhMsg: '很抱歉，服务器暂时繁忙，请稍后再试',
    enMsg: 'Sorry, the server is temporarily busy, please try again later',
  },
  default: {
    level: 'error',
    zhMsg: '很抱歉，请求出现问题，我们将尽快修复',
    enMsg: 'Sorry, there is a problem with the request, we will fix it as soon as possible',
  },
};

const errorHandler = (err) => {
  const { response, status } = err;
  const body = response && (response.body || response.data); // from superagent or axios
  const { level, enMsg, zhMsg } = statusMap[status] || statusMap.default;
  const backendMsg = body && body.err ? body.err.msg : null;
  const locale = window.localStorage.getItem('locale');
  const msg = locale === 'en' ? enMsg : zhMsg;

  // 因为csrf token过期导致的报错不进行提示，agent会进行重试请求
  if (err.status === 403 && (err.response.text || '').includes('empty csrf token')) {
    return;
  }

  notification[level]({
    message: locale === 'en' ? 'request error' : '请求错误',
    description: <pre className="whitespace-pre-line">{backendMsg || msg}</pre>,
    style: {
      width: 440,
      marginLeft: 385 - 440,
    },
  });

  if (status === 401) {
    userStore.effects.login();
  }
};

export default errorHandler;
