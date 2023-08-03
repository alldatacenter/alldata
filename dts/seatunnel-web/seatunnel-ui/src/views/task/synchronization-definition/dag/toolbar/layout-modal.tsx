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
import { NSelect, NForm, NFormItem, NInputNumber } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import Modal from '@/components/modal'
import type { PropType } from 'vue'

const props = {
  showModalRef: {
    type: Boolean as PropType<boolean>,
    default: false
  }
}

const LayoutModal = defineComponent({
  name: 'LayoutModal',
  props,
  emits: ['cancelModal', 'confirmModal'],
  setup(props, ctx) {
    const { t } = useI18n()
    const model = ref({
      layoutType: 'dagre',
      rows: 0,
      cols: 0
    })

    const resetModel = () => {
      model.value.layoutType = 'dagre'
      model.value.rows = 0
      model.value.cols = 0
    }

    const onCancelModal = () => {
      ctx.emit('cancelModal')
      resetModel()
    }

    const onConfirmModal = () => {
      ctx.emit(
        'confirmModal',
        model.value.layoutType as 'grid' | 'dagre',
        model.value.cols,
        model.value.rows
      )
      resetModel()
    }

    return {
      t,
      model,
      onCancelModal,
      onConfirmModal
    }
  },
  render() {
    return (
      <Modal
        title={this.t('project.synchronization_definition.format_canvas')}
        show={this.showModalRef}
        onCancel={this.onCancelModal}
        onConfirm={this.onConfirmModal}
      >
        <NForm ref='layoutFormRef' model={this.model}>
          <NFormItem
            label={this.t('project.synchronization_definition.layout_type')}
            path='layoutType'
          >
            <NSelect
              v-model={[this.model.layoutType, 'value']}
              options={[
                {
                  value: 'dagre',
                  label: this.t('project.synchronization_definition.dagre')
                },
                {
                  value: 'grid',
                  label: this.t('project.synchronization_definition.grid')
                }
              ]}
            />
          </NFormItem>
          {this.model.layoutType === 'grid' && (
            <>
              <NFormItem
                label={this.t('project.synchronization_definition.rows')}
                path='rows'
              >
                <NInputNumber v-model={[this.model.rows, 'value']} min={0} />
              </NFormItem>
              <NFormItem
                label={this.t('project.synchronization_definition.cols')}
                path='cols'
              >
                <NInputNumber v-model={[this.model.cols, 'value']} min={0} />
              </NFormItem>
            </>
          )}
        </NForm>
      </Modal>
    )
  }
})

export { LayoutModal }
