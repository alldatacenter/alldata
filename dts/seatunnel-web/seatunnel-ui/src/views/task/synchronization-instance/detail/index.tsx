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

import { defineComponent } from 'vue'
import { RunningInstance } from './running-instance'
import { TaskDefinition } from './task-definition'
import {
  NBreadcrumb,
  NBreadcrumbItem,
  NCard,
  NSpace,
  NTabPane,
  NTabs
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import type { Router } from 'vue-router'

const SynchronizationInstanceDetail = defineComponent({
  name: 'SynchronizationInstanceDetail',
  setup() {
    const { t } = useI18n()
    const router: Router = useRouter()
    const route = useRoute()

    return {
      t,
      router,
      route
    }
  },
  render() {
    return (
      <NSpace vertical>
        <NCard>
          <NBreadcrumb>
            <NBreadcrumbItem>
              <span
                onClick={() =>
                  this.router.push({
                    name: 'synchronization-instance',
                    params: { projectCode: this.route.params.projectCode },
                    query: {
                      project: this.route.query.project,
                      global: this.route.query.global
                    }
                  })
                }
              >
                {this.t('menu.synchronization_instance')}
              </span>
            </NBreadcrumbItem>
            <NBreadcrumbItem>{this.route.query.taskName}</NBreadcrumbItem>
          </NBreadcrumb>
        </NCard>
        <NTabs type='segment'>
          <NTabPane
            name='task-definition'
            tab={this.t(
              'project.synchronization_instance.sync_task_definition'
            )}
          >
            <TaskDefinition />
          </NTabPane>
          <NTabPane
            name='running-instance'
            tab={this.t(
              'project.synchronization_instance.data_pipeline_running_instance'
            )}
          >
            <RunningInstance />
          </NTabPane>
        </NTabs>
      </NSpace>
    )
  }
})

export default SynchronizationInstanceDetail
