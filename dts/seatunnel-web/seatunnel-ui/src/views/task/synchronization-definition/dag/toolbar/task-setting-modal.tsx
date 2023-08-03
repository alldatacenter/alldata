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

import { defineComponent, PropType, watch, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { NForm, NFormItem, NInput, NSelect, NSpin } from 'naive-ui'
import { useTaskSettingModal } from './use-task-setting-modal'
import { DynamicFormItem } from '@/components/dynamic-form/dynamic-form-item'
import Modal from '@/components/modal'

const props = {
  show: {
    type: Boolean as PropType<boolean>,
    default: false
  }
}

const TaskSettingModal = defineComponent({
  name: 'TaskSettingModal',
  props,
  emits: ['cancelModal'],
  setup(props, ctx) {
    const { t } = useI18n()
    const { state, settingFormRef, handleValidate, settingInit } =
      useTaskSettingModal(ctx)

    const onCancelModel = () => {
      ctx.emit('cancelModal')
    }

    const onConfirmModel = () => {
      handleValidate()
    }

    watch(
      () => props.show,
      () => {
        props.show && settingInit()
      }
    )

    return () => (
      <Modal
        title={t('project.synchronization_definition.setting')}
        show={props.show}
        onCancel={onCancelModel}
        onConfirm={onConfirmModel}
        confirmLoading={state.saving}
      >
        <NSpin show={state.loading}>
          <NForm model={state.model} rules={state.rules} ref={settingFormRef}>
            <NFormItem
              label={t('project.synchronization_definition.task_name')}
              path='taskName'
            >
              <NInput
                v-model={[state.model.taskName, 'value']}
                placeholder={t(
                  'project.synchronization_definition.task_name_placeholder'
                )}
                clearable
              />
            </NFormItem>
            <NFormItem
              label={t('project.synchronization_definition.description')}
              path='description'
            >
              <NInput
                v-model={[state.model.description, 'value']}
                placeholder={t(
                  'project.synchronization_definition.description_placeholder'
                )}
                clearable
              />
            </NFormItem>
            <NFormItem
              label={t('project.synchronization_definition.engine')}
              path='engine'
            >
              <NSelect
                v-model={[state.model.engine, 'value']}
                options={[{ value: 'SeaTunnel', label: 'SeaTunnel' }]}
              />
            </NFormItem>
            {state.formStructure.length > 0 && (
              <DynamicFormItem
                model={state.model}
                formStructure={state.formStructure}
                name={state.formName}
              />
            )}
          </NForm>
        </NSpin>
      </Modal>
    )
  }
})

export { TaskSettingModal }
