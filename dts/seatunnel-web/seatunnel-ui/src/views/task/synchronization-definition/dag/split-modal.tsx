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

import { defineComponent, PropType, ref } from 'vue'
import { NForm, NFormItem, NInput } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import type { ModelRecord } from './types'

const SplitModal = defineComponent({
  name: 'SplitModal',
  props: {
    rowData: {
      type: Object as PropType<ModelRecord>,
      default: {}
    },
    outputData: {
      type: Array as PropType<Array<any>>,
      default: []
    }
  },
  setup(props, { expose }) {
    const { t } = useI18n()
    const fieldsRef = ref()
    const separatorRef = ref()

    expose({
      getFields: () => ({
        outputFields: fieldsRef.value.split(','),
        separator: separatorRef.value,
        original_field: props.rowData.name
      })
    })

    return () => (
      <NForm>
        <NFormItem
          label={t('project.synchronization_definition.separator')}
          path='separator'
        >
          <NInput
            type='text'
            autofocus
            placeholder={t('project.synchronization_definition.separator_tips')}
            v-model={[separatorRef.value, 'value']}
          />
        </NFormItem>
        <NFormItem
          label={t('project.synchronization_definition.original_field')}
        >
          {props.rowData.name}
        </NFormItem>
        <NFormItem
          label={t('project.synchronization_definition.segmented_fields')}
        >
          <NInput type='text' v-model={[fieldsRef.value, 'value']} placeholder={t('project.synchronization_definition.segmented_fields_placeholder')}/>
        </NFormItem>
      </NForm>
    )
  }
})

export default SplitModal
