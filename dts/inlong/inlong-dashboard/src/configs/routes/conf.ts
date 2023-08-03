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

import type { RouteProps } from '.';

const conf: RouteProps[] = [
  {
    path: '/login',
    component: () => import('@/ui/pages/Login'),
  },
  {
    path: '/group',
    component: () => import('@/ui/pages/GroupDashboard'),
    childRoutes: [
      {
        path: '/create/:id?',
        component: () => import('@/ui/pages/GroupDetail'),
      },
      {
        path: '/detail/:id',
        component: () => import('@/ui/pages/GroupDetail'),
      },
    ],
  },
  {
    path: '/consume',
    component: () => import('@/ui/pages/ConsumeDashboard'),
    childRoutes: [
      {
        path: '/create/:id?',
        component: () => import('@/ui/pages/ConsumeDetail'),
      },
      {
        path: '/detail/:id',
        component: () => import('@/ui/pages/ConsumeDetail'),
      },
    ],
  },
  {
    path: '/process',
    childRoutes: [
      {
        path: '/:type?',
        component: () => import('@/ui/pages/Process'),
        childRoutes: [
          {
            path: '/:id',
            component: () => import('@/ui/pages/ProcessDetail'),
          },
        ],
      },
      {
        path: '/detail/:id',
        component: () => import('@/ui/pages/ProcessDetail'),
      },
    ],
  },
  {
    path: '/user',
    component: () => import('@/ui/pages/UserManagement'),
  },
  {
    path: '/approval',
    component: () => import('@/ui/pages/ProcessManagement'),
  },
  {
    path: '/clusters',
    component: () => import('@/ui/pages/Clusters'),
    childRoutes: [
      {
        path: '/node',
        component: () => import('@/ui/pages/Clusters/NodeManage'),
      },
    ],
  },
  {
    path: '/clusterTags',
    component: () => import('@/ui/pages/ClusterTags'),
  },
  {
    path: '/node',
    component: () => import('@/ui/pages/Nodes'),
  },
  {
    path: '/sync',
    component: () => import('@/ui/pages/SynchronizeDashboard'),
    childRoutes: [
      {
        path: '/create/:id?',
        component: () => import('@/ui/pages/SynchronizeDetail'),
      },
      {
        path: '/detail/:id',
        component: () => import('@/ui/pages/SynchronizeDetail'),
      },
    ],
  },
  {
    path: '/tenant',
    component: () => import('@/ui/pages/TenantManagement'),
  },
  {
    component: () => import('@/ui/pages/Error/404'),
  },
];

export default conf;
