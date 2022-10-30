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

import i18n from '@/i18n';

export interface MenuItemType {
  name: string;
  children?: MenuItemType[];
  path?: string;
  isAdmin?: boolean;
}

const menus: MenuItemType[] = [
  {
    path: '/group',
    name: i18n.t('configs.menus.Groups'),
  },
  {
    path: '/consume',
    name: i18n.t('configs.menus.DataConsumption'),
  },
  {
    name: i18n.t('configs.menus.Clusters'),
    children: [
      {
        path: '/clusters',
        name: i18n.t('configs.menus.Clusters'),
      },
      {
        path: '/clusterTags',
        name: i18n.t('configs.menus.ClusterTags'),
      },
    ],
  },
  {
    path: '/node',
    name: i18n.t('configs.menus.Node'),
  },
  {
    path: '/process',
    name: i18n.t('configs.menus.ApprovalManagement'),
  },
  {
    name: i18n.t('configs.menus.SystemManagement'),
    isAdmin: true,
    children: [
      {
        path: '/user',
        name: i18n.t('configs.menus.UserManagement'),
      },
      {
        path: '/approval',
        name: i18n.t('configs.menus.ResponsibleManagement'),
      },
    ],
  },
];

export default menus;
