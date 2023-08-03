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
import { defineComponent, PropType, toRef, ref, onMounted } from 'vue'
import {
  NTable,
  NText,
  NTooltip,
  NFormItem,
  NInput,
  NSelect,
  NSpace,
  NButton,
  NPopconfirm,
  NIcon,
  NEmpty
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { EditOutlined, DeleteOutlined } from '@vicons/antd'
import styles from './index.module.scss'
//import type { IDetailTableRecord } from './types'

const EditRow = defineComponent({
  name: 'EditRow',
  props: {
    row: {
      type: Object as PropType<any>,
      default: {}
    },
    plain: {
      type: Boolean,
      default: false
    },
    fieldTypes: {
      type: Array as PropType<string[]>,
      default: []
    }
  },
  emits: ['updateValue', 'delete'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const inputRef = ref()
    const fieldTypeRef = ref()
    const onUpdateValue = (field: keyof any, value: any) => {
      emit('updateValue', field, value)
    }
    onMounted(() => {
      inputRef.value.focus()
    })
    return () => (
      <tr>
        <td>
          <NFormItem
            showLabel={false}
            feedback={
              props.row.fieldName ? '' : t('virtual_tables.field_name_tips')
            }
            validation-status={props.row.fieldName ? '' : 'error'}
          >
            <NInput
              autofocus
              value={props.row.fieldName}
              clearable
              onUpdateValue={(value) => void onUpdateValue('fieldName', value)}
              ref={inputRef}
            />
          </NFormItem>
        </td>
        <td>
          <NFormItem showLabel={false} v-show={props.row.isEdit}>
            <NSelect
              ref={fieldTypeRef}
              value={props.row.fieldType}
              options={props.fieldTypes.map((item) => ({
                label: item,
                value: item
              }))}
              filterable
              onUpdateValue={(value) => void onUpdateValue('fieldType', value)}
            />
          </NFormItem>
        </td>
        <td>
          <NFormItem showLabel={false}>
            <NSelect
              value={props.row.nullable as number}
              options={[
                { value: 1, label: t('virtual_tables.yes') },
                { value: 0, label: t('virtual_tables.no') }
              ]}
              onUpdateValue={(value) => void onUpdateValue('nullable', value)}
            />
          </NFormItem>
        </td>
        <td>
          <NFormItem showLabel={false}>
            <NSelect
              value={props.row.primaryKey as number}
              options={[
                { value: 1, label: t('virtual_tables.yes') },
                { value: 0, label: t('virtual_tables.no') }
              ]}
              onUpdateValue={(value) => void onUpdateValue('primaryKey', value)}
            />
          </NFormItem>
        </td>
        <td>
          <NFormItem showLabel={false}>
            <NInput
              value={props.row.fieldComment}
              clearable
              onUpdateValue={(value) =>
                void onUpdateValue('fieldComment', value)
              }
            />
          </NFormItem>
        </td>
        <td>
          <NFormItem showLabel={false}>
            <NInput
              value={props.row.defaultValue}
              clearable
              onUpdateValue={(value) =>
                void onUpdateValue('defaultValue', value)
              }
            />
          </NFormItem>
        </td>
        {!props.plain && (
          <td>
            <NFormItem showLabel={false}>
              <NSpace align='start'>
                <NButton
                  size='small'
                  onClick={() => void onUpdateValue('isEdit', false)}
                >
                  {t('virtual_tables.cancel')}
                </NButton>
                <NButton
                  type='primary'
                  size='small'
                  onClick={() => {
                    if (!props.row.fieldName) {
                      inputRef.value.focus()
                      return
                    }
                    if (!props.row.fieldType) {
                      fieldTypeRef.value.focus()
                      return
                    }
                    onUpdateValue('isEdit', false)
                  }}
                >
                  {t('virtual_tables.confirm')}
                </NButton>
              </NSpace>
            </NFormItem>
          </td>
        )}
      </tr>
    )
  }
})

const StepTwoTable = defineComponent({
  name: 'StepTwoTable',
  props: {
    list: {
      type: Array as PropType<any[]>,
      default: []
    },
    plain: {
      type: Boolean,
      default: false
    },
    fieldTypes: {
      type: Array as PropType<string[]>,
      default: []
    }
  },
  setup(props) {
    const { t } = useI18n()
    const listRef = toRef(props, 'list')
    return () => (
      <NTable striped class={styles['step-two-table']}>
        <thead>
          <th>
            <NText type='error'>*</NText>
            {t('virtual_tables.field_name')}
          </th>
          <th class={styles['table-cell-center']}>
            <NText type='error'>*</NText>
            {t('virtual_tables.field_type')}
          </th>
          <th class={styles['table-cell-center']}>
            <NText type='error'>*</NText>
            {t('virtual_tables.is_null')}
          </th>
          <th class={styles['table-cell-center']}>
            <NText type='error'>*</NText>
            {t('virtual_tables.is_primary_key')}
          </th>
          <th>{t('virtual_tables.description')}</th>
          <th>{t('virtual_tables.default_value')}</th>
          {!props.plain && <th>{t('virtual_tables.operation')}</th>}
        </thead>
        <tbody>
          {listRef.value.map((row, index) =>
            row.isEdit && !props.plain ? (
              <EditRow
                key={row.key}
                row={row}
                plain={props.plain}
                fieldTypes={props.fieldTypes}
                onUpdateValue={(
                  field: keyof any,
                  value: any
                ) => {
                  // @ts-ignore
                  listRef.value[index][field] = value
                }}
                class={styles[row.isEdit ? 'edit-row' : 'plain-row']}
              />
            ) : (
              <tr>
                <td>
                  <span>{row.fieldName}</span>
                </td>
                <td class={styles['table-cell-center']}>
                  <span>{row.fieldType}</span>
                </td>
                <td class={styles['table-cell-center']}>
                  <span>
                    {row.nullable
                      ? t('virtual_tables.yes')
                      : t('virtual_tables.no')}
                  </span>
                </td>
                <td class={styles['table-cell-center']}>
                  <span>
                    {row.primaryKey
                      ? t('virtual_tables.yes')
                      : t('virtual_tables.no')}
                  </span>
                </td>
                <td>
                  <span>{row.fieldComment}</span>
                </td>
                <td>
                  <span>{row.defaultValue}</span>
                </td>
                {!props.plain && (
                  <td>
                    <NSpace align='start'>
                      <NTooltip>
                        {{
                          trigger: () => (
                            <NButton
                              type='primary'
                              circle
                              size='small'
                              onClick={() => {
                                listRef.value[index]['isEdit'] = true
                              }}
                            >
                              <NIcon>
                                <EditOutlined />
                              </NIcon>
                            </NButton>
                          ),
                          default: () => t('virtual_tables.edit')
                        }}
                      </NTooltip>
                      <NTooltip>
                        {{
                          trigger: () => (
                            <NPopconfirm
                              onPositiveClick={() => {
                                listRef.value.splice(index, 1)
                              }}
                            >
                              {{
                                trigger: () => (
                                  <NButton type='error' circle size='small'>
                                    <NIcon>
                                      <DeleteOutlined />
                                    </NIcon>
                                  </NButton>
                                ),
                                default: () => t('virtual_tables.delete_confirm')
                              }}
                            </NPopconfirm>
                          ),
                          default: () => t('virtual_tables.delete')
                        }}
                      </NTooltip>
                    </NSpace>
                  </td>
                )}
              </tr>
            )
          )}
          <tr v-show={!listRef.value.length}>
            <td colspan={6}>
              <NEmpty />
            </td>
          </tr>
        </tbody>
      </NTable>
    )
  }
})

export default StepTwoTable
