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

import { defineComponent, reactive } from 'vue'
import { NSpace, NCard, NButton, NIcon, NTooltip } from 'naive-ui'
import {
  CopyOutlined,
  CloseCircleOutlined,
  SaveOutlined,
  DeleteRowOutlined,
  FullscreenOutlined,
  FormatPainterOutlined,
  FullscreenExitOutlined,
  SettingOutlined
} from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import { useTextCopy } from '@/hooks'
import { useRoute, useRouter } from 'vue-router'
import { useFullscreen } from '@vueuse/core'
import { LayoutModal } from './layout-modal'
import { TaskSettingModal } from './task-setting-modal'
import { useSynchronizationDefinitionStore } from '@/store/synchronization-definition'
import type { Router } from 'vue-router'

const DagToolbar = defineComponent({
  name: 'DagToolbar',
  emits: ['delete', 'save', 'layout'],
  setup(props, { emit }) {
    const state = reactive({
      showLayoutModal: false,
      showSettingModal: false
    })
    const { t } = useI18n()
    const { copy } = useTextCopy()
    const { isFullscreen, toggle } = useFullscreen()
    const router: Router = useRouter()
    const route = useRoute()
    const dagStore = useSynchronizationDefinitionStore()
    const onSave = () => {
      emit('save')
    }

    const onClose = () => {
      router.push({
        name: 'synchronization-definition',
        query: {
          project: route.query.project,
          global: route.query.global
        }
      })
    }

    const onDelete = () => {
      emit('delete')
    }

    const onLayout = (
      layoutType: 'grid' | 'dagre',
      cols: number,
      rows: number
    ) => {
      state.showLayoutModal = false
      emit('layout', layoutType, cols, rows)
    }

    return () => (
      <>
        <NCard>
          <NSpace justify='space-between'>
            <NSpace align='center'>
              <span>{dagStore.getDagInfo.name}</span>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      quaternary
                      circle
                      onClick={() => {
                        copy(dagStore.getDagInfo.name)
                      }}
                    >
                      <NIcon>
                        <CopyOutlined />
                      </NIcon>
                    </NButton>
                  ),
                  default: () => t('project.synchronization_definition.copy')
                }}
              </NTooltip>
            </NSpace>
            <NSpace>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      strong
                      secondary
                      circle
                      type='info'
                      onClick={() => void (state.showSettingModal = true)}
                    >
                      <NIcon>
                        <SettingOutlined />
                      </NIcon>
                    </NButton>
                  ),
                  default: () => t('project.synchronization_definition.setting')
                }}
              </NTooltip>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      strong
                      secondary
                      circle
                      type='error'
                      onClick={onDelete}
                    >
                      <NIcon>
                        <DeleteRowOutlined />
                      </NIcon>
                    </NButton>
                  ),
                  default: () => t('project.synchronization_definition.delete')
                }}
              </NTooltip>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      strong
                      secondary
                      circle
                      type='info'
                      onClick={toggle}
                    >
                      <NIcon>
                        {isFullscreen.value ? (
                          <FullscreenExitOutlined />
                        ) : (
                          <FullscreenOutlined />
                        )}
                      </NIcon>
                    </NButton>
                  ),
                  default: () =>
                    isFullscreen.value
                      ? t(
                          'project.synchronization_definition.close_full_screen'
                        )
                      : t('project.synchronization_definition.open_full_screen')
                }}
              </NTooltip>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      strong
                      secondary
                      circle
                      type='info'
                      onClick={() => void (state.showLayoutModal = true)}
                    >
                      <NIcon>
                        <FormatPainterOutlined />
                      </NIcon>
                    </NButton>
                  ),
                  default: () => t('project.synchronization_definition.format')
                }}
              </NTooltip>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      strong
                      secondary
                      circle
                      type='success'
                      onClick={onSave}
                    >
                      <NIcon>
                        <SaveOutlined />
                      </NIcon>
                    </NButton>
                  ),
                  default: () => t('project.synchronization_definition.save')
                }}
              </NTooltip>
              <NTooltip trigger='hover'>
                {{
                  trigger: () => (
                    <NButton
                      strong
                      secondary
                      circle
                      type='error'
                      onClick={onClose}
                    >
                      <NIcon>
                        <CloseCircleOutlined />
                      </NIcon>
                    </NButton>
                  ),
                  default: () => t('project.synchronization_definition.close')
                }}
              </NTooltip>
            </NSpace>
          </NSpace>
        </NCard>
        <LayoutModal
          showModalRef={state.showLayoutModal}
          onCancelModal={() => void (state.showLayoutModal = false)}
          onConfirmModal={onLayout}
        />
        <TaskSettingModal
          show={state.showSettingModal}
          onCancelModal={() => void (state.showSettingModal = false)}
        />
      </>
    )
  }
})

export { DagToolbar }
