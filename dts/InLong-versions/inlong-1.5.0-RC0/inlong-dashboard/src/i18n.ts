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
      'configs.menus.Process': 'Approval',
      'configs.menus.Groups': 'Integration',
      'configs.menus.Subscribe': 'Subscription',
      'configs.menus.Clusters': 'Clusters',
      'configs.menus.ClusterTags': 'ClusterTags',
      'configs.menus.SystemManagement': 'System',
      'configs.menus.UserManagement': 'User Management',
      'configs.menus.ProcessManagement': 'Process Management',
      'configs.menus.Nodes': 'Nodes',
    },
  },
  cn: {
    translation: {
      'configs.menus.Process': '审批管理',
      'configs.menus.Groups': '数据集成',
      'configs.menus.Subscribe': '数据订阅',
      'configs.menus.Clusters': '集群管理',
      'configs.menus.ClusterTags': '标签管理',
      'configs.menus.SystemManagement': '系统管理',
      'configs.menus.UserManagement': '用户管理',
      'configs.menus.ProcessManagement': '流程管理',
      'configs.menus.Nodes': '节点管理',
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
