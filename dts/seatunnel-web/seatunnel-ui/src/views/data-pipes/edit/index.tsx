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
import {
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NCard,
  NSpace,
  NInput,
  NIcon,
  NTooltip
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { BulbOutlined } from '@vicons/antd'
import { scriptDetail, scriptUpdate } from '@/service/script'
import MonacoEditor from '@/components/monaco-editor'
import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'
import type { ScriptDetail } from '@/service/script/types'

const DataPipesEdit = defineComponent({
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

    const handleAdd = () => {
      scriptUpdate(Number(route.params.dataPipeId), variables.content).then(
        () => {
          handleClickDataPipes()
        }
      )
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

    return { t, ...toRefs(variables), handleClickDataPipes, handleAdd }
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
            ),
            'header-extra': () => (
              <NSpace>
                <NButton secondary onClick={this.handleClickDataPipes}>
                  {this.t('data_pipes.cancel')}
                </NButton>
                <NButton secondary type='primary' onClick={this.handleAdd}>
                  {this.t('data_pipes.save')}
                </NButton>
              </NSpace>
            )
          }}
        </NCard>
        <NCard>
          <NSpace align='center'>
            <span>{this.t('data_pipes.name')}</span>
            <NSpace align='center'>
              <NInput
                disabled
                maxlength='100'
                showCount
                style={{ width: '600px' }}
                v-model={[this.name, 'value']}
              />
              <NTooltip placement='right' trigger='hover'>
                {{
                  default: () => <span>{this.t('data_pipes.name_tips')}</span>,
                  trigger: () => (
                    <NIcon size='20' style={{ cursor: 'pointer' }}>
                      <BulbOutlined />
                    </NIcon>
                  )
                }}
              </NTooltip>
            </NSpace>
          </NSpace>
        </NCard>
        <NCard>
          <MonacoEditor v-model={[this.content, 'value']} />
        </NCard>
      </NSpace>
    )
  }
})

export default DataPipesEdit
