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
import { defineComponent, ref, toRef } from 'vue'
import {
  NForm,
  NFormItem,
  NSelect,
  NInput,
  SelectOption,
  SelectGroupOption
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSource } from '../datasource/list/use-source'
import { useTable } from '../datasource/list/use-table'
import styles from './index.module.scss'

const StepOneForm = defineComponent({
  name: 'StepOneForm',
  props: {
    params: {
      type: Object,
      default: {}
    }
  },
  setup(props, { expose }) {
    const { t } = useI18n()
    const { state: sourceState } = useSource(true)
    const { data: datasourceState, getList } = useTable()
    const rules = {
      pluginName: {
        required: true,
        trigger: ['input', 'blur'],
        message: t('virtual_tables.source_type_tips')
      },
      datasourceName: {
        required: true,
        trigger: ['input', 'blur'],
        message: t('virtual_tables.source_name_tips')
      },
      tableName: {
        required: true,
        trigger: ['input', 'blur'],
        message: t('virtual_tables.virtual_tables_name_tips')
      }
    }
    const paramsRef = toRef(props, 'params')
    const stepOneFormRef = ref()

    const handleUpdateType = (type: string) => {
      if (type === paramsRef.value.pluginName) return
      datasourceState.pageSize = 99999
      getList(type)
      paramsRef.value.datasourceName = null
      paramsRef.value.tableName = null
    }

    expose({
      validate: async () => {
        await stepOneFormRef.value.validate()
      }
    })

    return () => (
      <NForm
        rules={rules}
        ref={stepOneFormRef}
        require-mark-placement='left'
        label-align='right'
        labelPlacement='left'
        model={paramsRef.value}
        labelWidth={100}
      >
        <NFormItem
          label={t('virtual_tables.source_type')}
          path='pluginName'
          show-require-mark
        >
          <NSelect
            v-model:value={props.params.pluginName}
            filterable
            placeholder={t('virtual_tables.source_type_tips')}
            options={
             sourceState.types as Array<SelectGroupOption | SelectOption>
            }
            loading={sourceState.loading}
            class={styles['type-width']}
            onUpdateValue={(value) => void handleUpdateType(value)}
          />
        </NFormItem>
        <NFormItem
          label={t('virtual_tables.source_name')}
          path='datasourceName'
          show-require-mark
        >
          <NSelect
            v-model:value={paramsRef.value.datasourceName}
            filterable
            placeholder={t('virtual_tables.source_name_tips')}
            options={datasourceState.list.map(
              (item: { datasourceName: string; id: string }) => ({
                label: item.datasourceName,
                value: item.datasourceName,
                id: item.id
              })
            )}
            //loading={datasourceState.loading}
            class={styles['type-width']}
            onUpdateValue={(value, option: { id: string }) => {
              paramsRef.value.datasourceId = option.id
            }}
          />
        </NFormItem>
        <NFormItem
          label={t('virtual_tables.virtual_tables_name')}
          path='tableName'
          show-require-mark
        >
          <NInput
            v-model:value={paramsRef.value.tableName}
            clearable
            placeholder={t('virtual_tables.virtual_tables_name_tips')}
            class={styles['type-width']}
          />
        </NFormItem>
      </NForm>
    )
  }
})

export default StepOneForm
