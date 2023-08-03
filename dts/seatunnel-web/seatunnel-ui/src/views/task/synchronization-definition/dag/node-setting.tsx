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

import { defineComponent, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NDrawer,
  NDrawerContent,
  NSpace,
  NButton,
  NTabs,
  NTabPane
} from 'naive-ui'
import { useNodeSettingModal } from './use-node-setting'
import NodeModeModal from './node-model'
import ConfigurationForm from './configuration-form'
import type { PropType } from 'vue'
import type { NodeInfo } from './types'

const props = {
  show: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  nodeInfo: {
    type: Object as PropType<NodeInfo>,
    default: {}
  }
}

const NodeSetting = defineComponent({
  name: 'SettingNodeModal',
  props,
  emits: ['cancelModal', 'confirmModal'],
  setup(props, ctx) {
    const { t } = useI18n()
    const {
      state,
      configurationFormRef,
      modelRef,
      onSave,
      handleTab,
      handleChangeTable
    } = useNodeSettingModal(props, ctx)

    const cancelModal = () => {
      state.tab = 'configuration'
      state.width = '60%'
      ctx.emit('cancelModal', props.show)
    }

    watch(
      () => props.show,
      async () => {
        await nextTick()

        if (props.show && configurationFormRef.value) {
          await configurationFormRef.value.setValues(props.nodeInfo)
        }
        if (props.show && modelRef.value) {
          modelRef.value.setSelectFields(
            props.nodeInfo.selectTableFields?.tableFields || []
          )
        }
      }
    )

    return () => (
      <NDrawer show={props.show} width={state.width} zIndex={1000}>
        <NDrawerContent>
          {{
            default: () => (
              <NTabs onUpdateValue={handleTab} value={state.tab}>
                <NTabPane
                  name='configuration'
                  tab={t('project.synchronization_definition.configuration')}
                  displayDirective='show'
                >
                  <ConfigurationForm
                    nodeType={props.nodeInfo.type}
                    nodeId={props.nodeInfo.pluginId}
                    transformType={props.nodeInfo.connectorType}
                    ref={configurationFormRef}
                    onTableNameChange={handleChangeTable}
                  />
                </NTabPane>
                <NTabPane
                  name='model'
                  tab={t('project.synchronization_definition.model')}
                  displayDirective='show'
                >
                  <NodeModeModal
                    ref={modelRef}
                    type={props.nodeInfo.type}
                    transformType={props.nodeInfo.connectorType}
                    predecessorsNodeId={props.nodeInfo.predecessorsNodeId}
                    currentNodeId={props.nodeInfo.pluginId}
                    schemaError={props.nodeInfo.schemaError}
                    refForm={configurationFormRef}
                  />
                </NTabPane>
              </NTabs>
            ),
            footer: () => (
              <NSpace>
                <NButton onClick={cancelModal}>
                  {t('project.synchronization_definition.cancel')}
                </NButton>
                <NButton onClick={onSave} type='primary'>
                  {t('project.synchronization_definition.confirm')}
                </NButton>
              </NSpace>
            )
          }}
        </NDrawerContent>
      </NDrawer>
    )
  }
})

export default NodeSetting
