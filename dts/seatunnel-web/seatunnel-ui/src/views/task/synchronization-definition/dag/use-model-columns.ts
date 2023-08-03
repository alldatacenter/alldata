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

import { h, ref } from 'vue'
import {
  NFormItem,
  NInput,
  NSpace,
  NButton,
  NIcon,
  NDropdown,
  NPopconfirm,
  NTooltip,
  NEllipsis,
  useDialog
} from 'naive-ui'
import {
  EditOutlined,
  SwapOutlined,
  DeleteOutlined,
  ColumnHeightOutlined,
  CopyOutlined
} from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import {
  COLUMN_WIDTH_CONFIG,
  calculateTableWidth
} from '@/common/column-width-config'
import { cloneDeep, remove, max } from 'lodash'
import SplitModal from './split-modal'
import type { TableColumns, ModelRecord } from './types'
import { TableColumn } from 'naive-ui/es/data-table/src/interface'

export const useModelColumns = () => {
  const { t } = useI18n()
  const dialog = useDialog()
  const splitModalRef = ref()
  const handleSplit = (rowData: ModelRecord, outputData: ModelRecord[]) => {
    dialog.create({
      title: t('project.synchronization_definition.split_field'),
      content: () => h(SplitModal, { rowData, outputData, ref: splitModalRef }),
      onPositiveClick: () => {
        const { outputFields, separator, original_field } = splitModalRef.value.getFields()

        outputFields.forEach((field: string) => {
          outputData.push({
            ...rowData,
            name: field,
            isSplit: true,
            separator,
            original_field
          })
        })

        // For fields that have been split, the button is grayed out.
        const sourceGroup = outputData.filter((o: any) => !o.isSplit).map((o: any) => o.name)
        const splitGroup = outputData.filter((o: any) => o.isSplit).map((o: any) => o.original_field)

        splitGroup.forEach((s: any) => {
          if (sourceGroup.indexOf(s) >= 0) {
            outputData[sourceGroup.indexOf(s)].splitDisabled = true
          }
        })
      },
      positiveText: t('project.synchronization_definition.confirm'),
      negativeText: t('project.synchronization_definition.cancel')
    })
  }
  const createColumns = ({
    nodeType,
    columnSelectable,
    transformType,
    outputTableData
  }: {
    nodeType: string
    columnSelectable: boolean
    transformType: string
    outputTableData: ModelRecord[]
  }) => {
    const basicColumns = [
      {
        title: '#',
        key: 'index',
        render: (row: any, index: number) => index + 1,
        ...COLUMN_WIDTH_CONFIG['index']
      },
      {
        title: t('project.synchronization_definition.field_name'),
        key: 'name',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      {
        title: t('project.synchronization_definition.field_type'),
        key: 'type',
        ...COLUMN_WIDTH_CONFIG['type'],
        ellipsis: {
          tooltip: true
        }
      },
      {
        title: t('project.synchronization_definition.non_empty'),
        key: 'nullable',
        ...COLUMN_WIDTH_CONFIG['index'],
        render: (row: any) =>
          row.nullable
            ? t('project.synchronization_definition.yes')
            : t('project.synchronization_definition.no')
      },
      {
        title: t('project.synchronization_definition.primary_key'),
        key: 'primaryKey',
        ...COLUMN_WIDTH_CONFIG['index'],
        render: (row: any) =>
          row.primaryKey
            ? t('project.synchronization_definition.yes')
            : t('project.synchronization_definition.no')
      },
      {
        title: t('project.synchronization_definition.field_comment'),
        key: 'comment',
        ...COLUMN_WIDTH_CONFIG['name'],
        ellipsis: {
          tooltip: true
        }
      },
      {
        title: t('project.synchronization_definition.default_value'),
        key: 'defaultValue',
        ...COLUMN_WIDTH_CONFIG['state'],
        ellipsis: {
          tooltip: true
        }
      }
    ] as any[]

    const selection = {
      type: 'selection',
      disabled: (row: any) => {
        return row.unSupport
      },
      ...COLUMN_WIDTH_CONFIG['selection']
    }

    if (nodeType === 'transform' && transformType === 'FieldMapper') {
      const outputColumns = cloneDeep(basicColumns)
      const nameColumn = {
        title: t('project.synchronization_definition.field_name'),
        key: 'name',
        ...COLUMN_WIDTH_CONFIG['name'],
        render(row, index) {
          return row.isEdit
            ? h(
                NFormItem,
                {
                  showLabel: false,
                  validationStatus: !row.name ? 'error' : 'success',
                  feedback: !row.name
                    ? t('project.synchronization_definition.name_tips')
                    : ''
                },
                () =>
                  h(NInput, {
                    value: outputTableData[index].name,
                    onUpdateValue(v) {
                      outputTableData[index].name = v
                    },
                    onBlur() {
                      outputTableData[index].isEdit = false
                    }
                  })
              )
            : // @ts-ignore
              h('div', { align: 'center', style: { display: 'flex', 'flex-wrap': 'no-wrap', 'align-items': 'center' } }, [
                h(NEllipsis, {}, [row.name as any]),
                h(
                  NButton,
                  {
                    quaternary: true,
                    circle: true,
                    size: 'small',
                    onClick() {
                      outputTableData[index].isEdit = true
                    }
                  },
                  { icon: h(NIcon, null, () => h(EditOutlined)) }
                )
              ])
        }
      } as TableColumn
      outputColumns.splice(
        1,
        1,
        {
          title: t('project.synchronization_definition.original_field'),
          key: 'original_field',
          ...COLUMN_WIDTH_CONFIG['name'],
          render: (row: any) => {
            return h('span', { class: row.isError && 'row-error' }, row.original_field)
          }
        },
        nameColumn
      )
      outputColumns.push({
        title: t('project.synchronization_definition.operation'),
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](2),
        render(row: any, index: number) {
          return h(NSpace, {}, [
            h(
              NDropdown,
              {
                options: [
                  {
                    label: t('project.synchronization_definition.move_to_top'),
                    disabled: index === 0,
                    key: 'move_to_top'
                  },
                  {
                    label: t(
                      'project.synchronization_definition.move_to_bottom'
                    ),
                    disabled: index === outputTableData.length - 1,
                    key: 'move_to_bottom'
                  },
                  {
                    label: t('project.synchronization_definition.move_up'),
                    disabled: index === 0,
                    key: 'move_up'
                  },
                  {
                    label: t('project.synchronization_definition.move_down'),
                    disabled: index === outputTableData.length - 1,
                    key: 'move_down'
                  }
                ],
                onSelect(key) {
                  if (key === 'move_to_top') {
                    const records = outputTableData.splice(index, 1)
                    outputTableData.unshift(records[0])
                  } else if (key === 'move_to_bottom') {
                    const records = outputTableData.splice(index, 1)
                    outputTableData.push(records[0])
                  } else if (key === 'move_up') {
                    changeIndex(outputTableData, index, index - 1)
                  } else if (key === 'move_down') {
                    changeIndex(outputTableData, index, index + 1)
                  }
                }
              },
              h(
                NButton,
                { circle: true, size: 'small', type: 'info' },
                h(NIcon, { style: 'transform: rotate(90deg)' }, () =>
                  h(SwapOutlined)
                )
              )
            ),
            h(
              NPopconfirm,
              {
                onPositiveClick() {
                  outputTableData.splice(index, 1)
                }
              },
              {
                trigger: () =>
                  h(
                    NButton,
                    { circle: true, size: 'small', type: 'error' },
                    h(NIcon, {}, () => h(DeleteOutlined))
                  ),
                default: () =>
                  t('project.synchronization_definition.delete_confirm')
              }
            )
          ])
        }
      })
      return {
        inputColumns: !columnSelectable
          ? basicColumns
          : ([selection, ...basicColumns.slice(1)] as TableColumns),
        outputColumns: outputColumns,
        inputTableWidth: calculateTableWidth(basicColumns),
        outputTableWidth: calculateTableWidth(outputColumns)
      }
    }

    if (nodeType === 'transform' && transformType === 'MultiFieldSplit') {
      const outputColumns = cloneDeep(basicColumns)
      const nameColumn = {
        title: t('project.synchronization_definition.field_name'),
        key: 'name',
        ...COLUMN_WIDTH_CONFIG['name'],
        render: (row: any) => {
          // Mark the same field name value in red.
          const onlyNameColumnTableData = outputTableData.map((o: any) => o.name)
          return h('span', { class: onlyNameColumnTableData.indexOf(row.name) !== onlyNameColumnTableData.lastIndexOf(row.name)  && 'row-error' }, row.name)
        }
      } as TableColumn
      outputColumns.splice(
        1,
        1,
        {
          title: t('project.synchronization_definition.original_field'),
          key: 'original_field',
          ...COLUMN_WIDTH_CONFIG['name'],
          render: (row: any) => {
            return h('span', { class: row.isError && 'row-error' }, row.original_field)
          }
        },
        nameColumn
      )

      outputColumns.push({
        title: t('project.synchronization_definition.operation'),
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](1),
        width: 60,
        render(row: any, index: number) {
          return row.isSplit
            ? h(
                NPopconfirm,
                {
                  onPositiveClick() {
                    //outputTableData.splice(index, 1)

                    // When a piece of information is deleted, the entries that
                    // are divided at the same time as the piece of information
                    // will be deleted together.
                    remove(outputTableData, (i => (i.original_field === row.original_field) && i.isSplit))

                    // After all the split fields are deleted, the Unsplit button is grayed out.
                    const sourceGroup = outputTableData.filter((o: any) => !o.isSplit).map((o: any) => o.name)
                    const splitGroup = outputTableData.filter((o: any) => o.isSplit).map((o: any) => o.original_field)

                    sourceGroup.forEach((s: any) => {
                      if (splitGroup.indexOf(s) < 0) {
                        outputTableData[sourceGroup.indexOf(s)].splitDisabled = false
                      }
                    })
                  }
                },
                {
                  trigger: () =>
                    h(
                      NButton,
                      { circle: true, size: 'small', type: 'error' },
                      h(NIcon, {}, () => h(DeleteOutlined))
                    ),
                  default: () =>
                    t('project.synchronization_definition.delete_confirm')
                }
              )
            : h(
                NTooltip,
                {},
                {
                  trigger: () =>
                    h(
                      NButton,
                      {
                        circle: true,
                        size: 'small',
                        type: 'info',
                        disabled: row.splitDisabled,
                        onClick() {
                          handleSplit(row, outputTableData)
                        }
                      },
                      h(NIcon, {}, () => h(ColumnHeightOutlined))
                    ),
                  default: () => t('project.synchronization_definition.split')
                }
              )
        }
      })
      return {
        inputColumns: !columnSelectable
          ? basicColumns
          : ([selection, ...basicColumns.slice(1)] as TableColumns),
        outputColumns: outputColumns,
        inputTableWidth: calculateTableWidth(basicColumns),
        outputTableWidth: calculateTableWidth(outputColumns)
      }
    }

    if (nodeType === 'transform' && transformType === 'Copy') {
      const outputColumns = cloneDeep(basicColumns)

      const nameColumn = {
        title: t('project.synchronization_definition.field_name'),
        key: 'name',
        ...COLUMN_WIDTH_CONFIG['name'],
        render: (row: any, index) => {
          // Mark the same field name value in red.
          const onlyNameColumnTableData = outputTableData.map((o: any) => o.name)
          return row.copyTimes !== -1 ? 
                h('span', { class: onlyNameColumnTableData.indexOf(row.name) !== onlyNameColumnTableData.lastIndexOf(row.name)  && 'row-error' }, row.name)
                : row.isEdit ? 
                h(NFormItem,
                  {
                    showLabel: false,
                    validationStatus: !row.name ? 'error' : 'success',
                    feedback: !row.name
                      ? t('project.synchronization_definition.name_tips')
                      : ''
                  },
                  () =>
                    h(NInput, {
                      value: outputTableData[index].name,
                      onUpdateValue(v) {
                        outputTableData[index].name = v
                      },
                      onBlur() {
                        outputTableData[index].isEdit = false
                      }
                    })
                  )
                : // @ts-ignore
                h('div', { align: 'center', style: { display: 'flex', 'flex-wrap': 'no-wrap', 'align-items': 'center' } }, [
                  h(NEllipsis, {}, [row.name as any]),
                  h(
                    NButton,
                    {
                      quaternary: true,
                      circle: true,
                      size: 'small',
                      onClick() {
                        outputTableData[index].isEdit = true
                      }
                    },
                    { icon: h(NIcon, null, () => h(EditOutlined)) }
                  )
                ])
        }
      } as TableColumn

      outputColumns.splice(
        1,
        1,
        {
          title: t('project.synchronization_definition.original_field'),
          key: 'original_field',
          ...COLUMN_WIDTH_CONFIG['name'],
          render: (row: any) => {
            return row.original_field
          }
        },
        nameColumn
      )

      outputColumns.push({
        title: t('project.synchronization_definition.operation'),
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](1),
        width: 60,
        render(row: any, index: number) {
          return row.copyTimes === -1
            ? h(
                NPopconfirm,
                {
                  onPositiveClick() {
                    outputTableData.splice(index, 1)
                  }
                },
                {
                  trigger: () =>
                    h(
                      NButton,
                      { circle: true, size: 'small', type: 'error' },
                      h(NIcon, {}, () => h(DeleteOutlined))
                    ),
                  default: () =>
                    t('project.synchronization_definition.delete_confirm')
                }
              )
            : h(
                NTooltip,
                {},
                {
                  trigger: () =>
                    h(
                      NButton,
                      {
                        circle: true,
                        size: 'small',
                        type: 'info',
                        onClick() {
                          const result = outputTableData.filter(
                            (o: any) => (o.original_field === row.original_field) && (o.original_field !== o.name)
                          )

                          const maxCopyTimes: any = max(result.map((r: any) => Number(r.name.split(r.original_field)[1])))

                          outputTableData[index].copyTimes = (maxCopyTimes ?? 0) + 1
                          outputTableData.push({
                            ...row,
                            name: row.name + row.copyTimes,
                            copyTimes: -1
                          })
                        }
                      },
                      h(NIcon, {}, () => h(CopyOutlined))
                    ),
                  default: () =>
                    t('project.synchronization_definition.copy_field')
                }
              )
        }
      })
      return {
        inputColumns: !columnSelectable
          ? basicColumns
          : ([selection, ...basicColumns.slice(1)] as TableColumns),
        outputColumns: outputColumns,
        inputTableWidth: calculateTableWidth(basicColumns),
        outputTableWidth: calculateTableWidth(outputColumns)
      }
    }

    const sourceOutputCheckType = () => {
      const resultColumns = [...basicColumns]
      const type = {
        title: t('project.synchronization_definition.field_type'),
        key: 'type',
        ...COLUMN_WIDTH_CONFIG['type'],
        ellipsis: {
          tooltip: true
        },
        render: (row: any) => row.outputDataType ? row.outputDataType : row.type
      }
      resultColumns.splice(2, 1, type)
      return resultColumns
    }

    return {
      inputColumns: !columnSelectable
        ? basicColumns
        : ([selection, ...basicColumns.slice(1)] as TableColumns),
      outputColumns: nodeType !== 'source' ? basicColumns : sourceOutputCheckType(),
      inputTableWidth: calculateTableWidth(basicColumns),
      outputTableWidth: calculateTableWidth(basicColumns)
    }
  }
  return { createColumns }
}

function changeIndex(list: any[], targetIndex: number, sourceIndex: number) {
  const record = list[sourceIndex]
  list[sourceIndex] = list[targetIndex]
  list[targetIndex] = record
}
