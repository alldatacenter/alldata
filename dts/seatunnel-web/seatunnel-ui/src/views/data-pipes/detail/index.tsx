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

import { defineComponent, onMounted, reactive, toRefs } from 'vue'
import { NSpace, NCard, NBreadcrumb, NBreadcrumbItem } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { scriptDetail } from '@/service/script'
import MonacoEditor from '@/components/monaco-editor'
import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'
import type { ScriptDetail } from '@/service/script/types'

const DataPipesDetail = defineComponent({
  setup() {
    const { t } = useI18n()
    const router: Router = useRouter()
    const route: RouteLocationNormalizedLoaded = useRoute()
    const variables = reactive({
      name: '',
      type: 0,
      content: ''
    })

    const handleClickDataPipes = () => {
      router.push({ path: '/data-pipes/list' })
    }

    onMounted(() => {
      scriptDetail(Number(route.params.dataPipeId)).then(
        (res: ScriptDetail) => {
          variables.name = res.name
          variables.type = res.type
          variables.content = res.content
        }
      )
    })

    return { t, ...toRefs(variables), handleClickDataPipes }
  },
  render() {
    return (
      <NSpace vertical>
        <NCard>
          {{
            header: () => (
              <NSpace align='center'>
                <NBreadcrumb>
                  <NBreadcrumbItem onClick={this.handleClickDataPipes}>
                    {this.t('data_pipes.data_pipes')}
                  </NBreadcrumbItem>
                  <NBreadcrumbItem>{this.name}</NBreadcrumbItem>
                </NBreadcrumb>
              </NSpace>
            )
          }}
        </NCard>
        <NCard>
          <MonacoEditor
            v-model={[this.content, 'value']}
            options={{ readOnly: true }}
          />
        </NCard>
      </NSpace>
    )
  }
})

export default DataPipesDetail
