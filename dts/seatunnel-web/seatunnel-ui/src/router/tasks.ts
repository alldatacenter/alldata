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
  redirect: { name: 'synchronization-definition' },
  component: () => import('@/layouts/dashboard'),
  children: [
    {
      path: '/task/synchronization-definition',
      name: 'synchronization-definition',
      component: components['task-synchronization-definition'],
      meta: {
        title: 'synchronization-definition',
        activeMenu: 'tasks',
        activeSide: 'synchronization-definition',
        showSide: true
      }
    },
    {
      path: '/task/synchronization-definition/:jobDefinitionCode',
      name: 'synchronization-definition-dag',
      component: components['task-synchronization-definition-dag'],
      meta: {
        title: 'synchronization-definition-dag',
        activeMenu: 'tasks',
        activeSide: 'synchronization-definition',
        showSide: true,
      }
    },
    {
      path: '/task/synchronization-instance',
      name: 'synchronization-instance',
      component: components['task-synchronization-instance'],
      meta: {
        title: 'synchronization-instance',
        activeMenu: 'tasks',
        activeSide: 'synchronization-instance',
        showSide: true
      }
    },
    {
      path: '/task/synchronization-instance/:taskCode',
      name: 'synchronization-instance-detail',
      component: components['task-synchronization-instance-detail'],
      meta: {
        title: 'synchronization-instance-detail',
        activeMenu: 'tasks',
        activeSide: 'synchronization-instance',
        showSide: true,
      }
    }
  ]
}
