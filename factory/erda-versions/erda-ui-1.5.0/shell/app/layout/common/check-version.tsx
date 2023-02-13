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

/**
 * 应用版本检查，版本更新后页面还用的旧的chunk文件，可能会报错
 * 局限：
 * 1、非增量更新
 * 2、浏览器需要刷新页面
 */
import agent from 'agent';
import { message, notification, Button } from 'antd';
import React from 'react';
import { ErdaIcon } from 'common';
import i18n from 'i18n';

function getCurrentVersion() {
  return agent.get('/version.json').then((response: any) => response.body);
}

const openNotification = () => {
  notification.open({
    duration: 0,
    message: i18n.t('New version available'),
    description: i18n.t('Version has been updated, it is recommended to refresh the page'),
    icon: <ErdaIcon type="smiling-face-with-squinting-eyes" className="text-primary" />,
    btn: (
      <Button type="primary" size="small" onClick={() => location.reload()}>
        {i18n.t('refresh')}
      </Button>
    ),
  });
};

export function checkVersion() {
  window.addEventListener('unhandledrejection', (e) => {
    // 兜底方案，load chunk出错时强制刷新
    const msg = e.reason.message;
    if (msg && msg.indexOf('Loading') > -1 && msg.indexOf('chunk') > -1) {
      if (navigator.onLine) {
        window.location.reload();
      } else {
        message.error('亲，检查一下网络连接', 3);
      }
    }
    return true;
  });

  let currentVersion: any = null;
  let timeId: any = null;
  getCurrentVersion().then((response: any) => {
    // 初次获取版本号
    currentVersion = (response || {}).version;
  });
  timeId = setInterval(() => {
    getCurrentVersion().then((response: any) => {
      if (currentVersion === (response || {}).version) return;
      openNotification();
      clearInterval(timeId);
      timeId = null;
    });
  }, 5 * 60 * 1000); // 5min请求一下版本号
}
