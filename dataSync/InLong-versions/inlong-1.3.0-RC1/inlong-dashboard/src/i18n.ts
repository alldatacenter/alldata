/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getCurrentLocale } from '@/configs/locales';
import { isDevelopEnv } from '@/utils';

const resources = {
  en: {
    translation: {
      'configs.menus.ApprovalManagement': 'Approval',
      'configs.menus.Groups': 'Data Group',
      'configs.menus.DataConsumption': 'Data Consumption',
      'configs.menus.Datasources': 'Data Sources',
      'configs.menus.Clusters': 'Clusters',
      'configs.menus.ClusterTags': 'ClusterTags',
      'configs.menus.SystemManagement': 'System',
      'configs.menus.UserManagement': 'User',
      'configs.menus.ResponsibleManagement': 'ApprovalManagement',
      'configs.menus.Node': 'Nodes',
    },
  },
  cn: {
    translation: {
      'configs.menus.ApprovalManagement': '审批管理',
      'configs.menus.Groups': '数据分组',
      'configs.menus.DataConsumption': '数据消费',
      'configs.menus.Datasources': '数据源服务器',
      'configs.menus.Clusters': '集群管理',
      'configs.menus.ClusterTags': '标签管理',
      'configs.menus.SystemManagement': '系统管理',
      'configs.menus.UserManagement': '用户管理',
      'configs.menus.ResponsibleManagement': '审批责任人管理',
      'configs.menus.Node': '节点管理',
    },
  },
};

i18n
  // .use(lngDetector)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  // for all options read: https://www.i18next.com/overview/configuration-options
  .init({
    fallbackLng: 'en',
    resources,
    lng: getCurrentLocale(),
    debug: isDevelopEnv(),

    interpolation: {
      escapeValue: false, // not needed for react as it escapes by default
    },

    react: {
      useSuspense: false,
    },
  });

export default i18n;
