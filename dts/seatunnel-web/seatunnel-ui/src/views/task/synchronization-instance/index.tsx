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

import { defineComponent, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NSpace, NTabs, NTabPane } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { SyncTask } from './sync-task'

const SynchronizationInstance = defineComponent({
  name: 'SynchronizationInstance',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const { t } = useI18n()
    let syncTaskType = ref(route.query.syncTaskType || 'BATCH')
   
    return { t, syncTaskType }
  },
  render() {
    return (
      <NSpace vertical>
        <NTabs type='segment' v-model:value={this.syncTaskType}>
          <NTabPane
            name='BATCH'
            tab={this.t('project.synchronization_instance.offline_sync')}
          >
            <SyncTask syncTaskType='BATCH' />
          </NTabPane>
          <NTabPane
            name='STREAMING'
            tab={this.t('project.synchronization_instance.real_time_sync')}
          >
            <SyncTask syncTaskType='STREAMING' />
          </NTabPane>
        </NTabs>
      </NSpace>
    )
  }
})

export default SynchronizationInstance
