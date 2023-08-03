/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import utils from '@/utils'
import type { Component } from 'vue'

const modules = import.meta.glob('/src/views/**/**.tsx')
const components: { [key: string]: Component } = utils.mapping(modules)

export default {
  path: '/tasks',
  name: 'tasks',
  meta: {
    title: 'tasks'
  },
  redirect: { name: 'tasks-list' },
  component: () => import('@/layouts/dashboard'),
  children: [
    {
      path: '/task/synchronization-definition',
      name: 'synchronization-definition',
      component: components['projects-task-synchronization-definition'],
      meta: {
        title: '同步任务定义',
        activeMenu: 'projects',
        showSide: true
        // auth: 'project:seatunnel-task:view'
      }
    },
    {
      path: '/task/synchronization-definition/:jobDefinitionCode',
      name: 'synchronization-definition-dag',
      component: components['projects-task-synchronization-definition-dag'],
      meta: {
        title: '同步任务定义画布',
        activeMenu: 'projects',
        activeSide: '/task/synchronization-definition',
        showSide: true,
        auth: ['project:seatunnel-task:create', 'project:seatunnel-task:update']
      }
    },
    {
      path: '/task/synchronization-instance',
      name: 'synchronization-instance',
      component: components['projects-task-synchronization-instance'],
      meta: {
        title: '同步任务实例',
        activeMenu: 'projects',
        showSide: true
        // auth: 'project:seatunnel-task-instance:view'
      }
    },
    {
      path: '/task/synchronization-instance/:taskCode',
      name: 'synchronization-instance-detail',
      component: components['projects-task-synchronization-instance-detail'],
      meta: {
        title: '同步任务实例详情',
        activeMenu: 'projects',
        activeSide: '/task/synchronization-instance',
        showSide: true,
        auth: 'project:seatunnel-task-instance:details'
      }
    }
  ]
}
