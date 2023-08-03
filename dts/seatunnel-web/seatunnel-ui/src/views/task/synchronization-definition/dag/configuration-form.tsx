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

import { defineComponent, nextTick, PropType, ref, watchEffect } from 'vue'
import {
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSpace,
  NRadioGroup,
  NRadio,
  NCheckboxGroup,
  NCheckbox,
  NSpin,
  NTransfer
} from 'naive-ui'
import { DynamicFormItem } from '@/components/dynamic-form/dynamic-form-item'
import { KINDS } from './config'
import {
  useConfigurationForm,
  getSceneModeOptions
} from './use-configuration-form'
import { useI18n } from 'vue-i18n'
import type { NodeType } from './types'

import { debounce } from 'lodash'

const ConfigurationForm = defineComponent({
  name: 'ConfigurationForm',
  props: {
    // eslint-disable-next-line vue/require-default-prop
    nodeType: {
      type: String as PropType<string>
    },
    // eslint-disable-next-line vue/require-default-prop
    nodeId: {
      type: String as PropType<string>
    },
    // eslint-disable-next-line vue/require-default-prop
    transformType: {
      type: String as PropType<string>
    }
  },
  emits: ['tableNameChange'],
  setup(props, { expose, emit }) {
    const {
      state,
      dagStore,
      getDatasourceOptions,
      getDatabaseOptions,
      getTableOptions,
      updateFormValues
    } = useConfigurationForm(
      props.nodeType as NodeType,
      props.transformType as string
    )
    const { t } = useI18n()
    const formRef = ref()
    const transfer = ref()

    const onTableChange = (tableName: any) => {
      state.model.tableName = tableName
      emit('tableNameChange', state.model)
    }

    
    const prevQueryTableName = ref('');
    const onTableSearch = debounce((tableName: any) => {
      // rely on database
      if(state.model.database && prevQueryTableName.value !== tableName) {
        getTableOptions(state.model.database, tableName)
        prevQueryTableName.value = tableName
      }
    }, 1000)

    const onDatabaseChange = (v: any) => {
      nextTick(() => {
      if(state.model.database) {
        let size = state.model.sceneMode === 'MULTIPLE_TABLE' ? 9999999 : 100
          getTableOptions(state.model.database as any, '', size)
        }
      })
    }
    
    // watchEffect(() => {
    //   // Track the src input of the transfer and refresh the table name list when the input value change
    //   let query = transfer?.value?.srcPattern
    //   onTableSearch(query)      
    // })

    expose({
      validate: async () => {
        try {
          await formRef.value.validate()
          return true
        } catch (err) {
          return false
        }
      },
      getValues: () => state.model,
      setValues: updateFormValues
    })

    return () => (
      <NSpin show={state.loading}>
        <NForm ref={formRef} model={state.model} rules={state.rules}>
          <NFormItem
            label={t('project.synchronization_definition.node_name')}
            path='name'
          >
            <NInput
              clearable
              v-model={[state.model.name, 'value']}
              placeholder={t(
                'project.synchronization_definition.node_name_placeholder'
              )}
            />
          </NFormItem>

          {props.nodeType === 'source' && (
            <NFormItem
              label={t('project.synchronization_definition.scene_mode')}
              path='sceneMode'
            >
              <NSelect
                filterable
                options={getSceneModeOptions(dagStore.getDagInfo.jobType, t)}
                v-model={[state.model.sceneMode, 'value']}
                onUpdateValue={(v) => {
                  if (v !== state.model.sceneMode) {
                    getDatasourceOptions(v)
                    state.model.datasourceInstanceId = null
                    state.model.database = null
                    state.model.tableName = null
                    state.formStructure = []
                    state.databaseOptions = []
                    state.tableOptions = []
                  }
                }}
              />
            </NFormItem>
          )}

          {props.nodeType !== 'transform' && (
            <NFormItem
              label={t('project.synchronization_definition.source_name')}
              path='datasourceInstanceId'
            >
              <NSelect
                filterable
                loading={state.datasourceLoading}
                options={state.datasourceOptions}
                v-model={[state.model.datasourceInstanceId, 'value']}
                onUpdateValue={(v, option) => {
                  if (v !== state.model.datasourceInstanceId) {
                    getDatabaseOptions(v, option)
                    state.model.database = null
                    state.model.tableName = null
                    state.tableOptions = []
                  }
                }}
              />
            </NFormItem>
          )}

          {props.nodeType !== 'transform' && (
            <NFormItem
              label={t('project.synchronization_definition.database')}
              path='database'
            >
              <NSelect
                filterable
                loading={state.databaseLoading}
                multiple={state.model.sceneMode === 'SPLIT_TABLE'}
                options={state.databaseOptions}
                v-model={[state.model.database, 'value']}
                onUpdateValue={(v) => {
                  if (v !== state.model.database) {
                    onDatabaseChange(v)
                    state.model.tableName = null
                  }
                }}
              />
            </NFormItem>
          )}

          {dagStore.getDagInfo.jobType === 'DATA_INTEGRATION' &&
            (props.nodeType === 'sink' || props.nodeType === 'source') && (
              <NFormItem
                label={t('project.synchronization_definition.table_name')}
                path='tableName'
              >
                <NSelect
                  filterable
                  loading={state.tableLoading}
                  options={state.tableOptions}
                  v-model={[state.model.tableName, 'value']}
                  onUpdateValue={onTableChange}
                  onSearch={onTableSearch}
                  remote
                  virtualScroll
                  />
              </NFormItem>
            )}

          {state.model.sceneMode === 'MULTIPLE_TABLE' && (
            <NFormItem
              label={t('project.synchronization_definition.table_name')}
              path='tableName'
            >
              <NTransfer
                style={{ width: '100%' }}
                ref={transfer}
                filterable
                sourceTitle={t('project.synchronization_definition.table_sync')}
                targetTitle={t(
                  'project.synchronization_definition.selected_table'
                )}
                options={state.tableOptions}
                v-model={[state.model.tableName, 'value']}
                onUpdateValue={onTableChange}
                virtualScroll
              />
            </NFormItem>
          )}

          {props.transformType === 'FilterRowKind' && (
            <>
              <NFormItem
                label={t('project.synchronization_definition.kind')}
                path='kind'
                showFeedback={false}
                showRequireMark
              >
                <NRadioGroup
                  v-model={[state.model.kind, 'value']}
                  name='model.kind'
                >
                  <NSpace>
                    <NRadio value={0}>
                      {t('project.synchronization_definition.include_kind')}
                    </NRadio>
                    <NRadio value={1}>
                      {t('project.synchronization_definition.exclude_kind')}
                    </NRadio>
                  </NSpace>
                </NRadioGroup>
              </NFormItem>
              <NFormItem showLabel={false} path='kinds'>
                <NCheckboxGroup v-model={[state.model.kinds, 'value']}>
                  <NSpace>
                    {KINDS.map((kind) => (
                      <NCheckbox value={kind.value} label={kind.label} />
                    ))}
                  </NSpace>
                </NCheckboxGroup>
              </NFormItem>
            </>
          )}

          {props.transformType === 'Sql' && (
            <NFormItem
              label={t('project.synchronization_definition.sql_content_label')}
              path='query'
            >
              <NInput
                v-model={[state.model.query, 'value']}
                type="textarea"
                clearable
                placeholder={t(
                  'project.synchronization_definition.sql_content_label_placeholder'
                )}
              />
            </NFormItem>
          )}

          {state.formStructure.length > 0 && (
            <DynamicFormItem
              model={state.model}
              formStructure={state.formStructure}
              name={state.formName}
              locales={state.formLocales}
            />
          )}
        </NForm>
      </NSpin>
    )
  }
})

export default ConfigurationForm
