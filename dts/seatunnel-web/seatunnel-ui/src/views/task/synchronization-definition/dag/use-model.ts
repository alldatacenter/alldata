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

import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  queryTaskDetail,
  modelInfo,
  sqlModelInfo
} from '@/service/sync-task-definition'
import { DefaultTableWidth } from '@/common/column-width-config'
import { useModelColumns } from './use-model-columns'
import { useRoute } from 'vue-router'
import type { TableColumns, ModelRecord } from './types'

export function useNodeModel(
  type: string,
  transformType: string,
  predecessorsNodeId: string,
  schemaError: any,
  currentNodeId: string,
  refForm: any
) {
  const { createColumns } = useModelColumns()
  const route = useRoute()
  const state = reactive({
    inputColumns: [] as TableColumns,
    inputTableData: [] as ModelRecord[],
    inputLoading: false,
    outputColumns: [] as TableColumns,
    outputTableData: [] as ModelRecord[],
    allTableData: [],
    schemaError: {} as any,
    optionsOutputTableData: [],
    transformOptions: {} as any,
    secondTransformOptions: {} as any,
    outputLoading: false,
    currentTable: '',
    selectedKeys: [] as string[],
    tables: [] as string[],
    columnSelectable: false,
    database: '',
    datasourceInstanceId: '',
    datasourceName: '',
    inputTableWidth: DefaultTableWidth,
    outputTableWidth: DefaultTableWidth,
    format: ''
  })

  let tempOutputTables = [] as ModelRecord[]

  const getModelData = () => {
    // The sink or transform model does not have an output table structure, and
    // the input table structure will be rendered according to the upstream node
    // id to obtain the output table structure.
    if (type === 'sink' || type === 'transform') {

      // if sql-transform, request when sql is not empty
      if(transformType === 'Sql' && !refForm.value.getValues()?.query) return 
      // Request the data of the previous node.
      predecessorsNodeId && queryTaskDetail(
        route.params.jobDefinitionCode as string,
        predecessorsNodeId
      ).then((res: any) => {
        state.tables = res.outputSchema.map((o: any) => o.tableName)
        state.currentTable = res.outputSchema[0].tableName
        state.allTableData = [{ database: res.outputSchema[0].database, tableInfos: res.outputSchema }] as any
        if (res.transformOptions) {
          state.transformOptions = res.transformOptions
        }
        onSwitchTable(state.currentTable)
      })

    } else {
      modelInfo(state.datasourceInstanceId, [{
        database: state.database,
        tables: state.tables
      }]).then((res: any) => {
        state.allTableData = res
        onSwitchTable(state.currentTable)
      })
    }
  }

  const getColumns = () => {
    const { inputColumns, outputColumns, inputTableWidth, outputTableWidth } =
      createColumns({
        nodeType: type,
        columnSelectable: state.columnSelectable,
        transformType,
        outputTableData: state.outputTableData
      }) as {
        inputColumns: TableColumns
        outputColumns: TableColumns
        inputTableWidth: number
        outputTableWidth: number
        outputTableData: ModelRecord[]
      }
    state.inputColumns = inputColumns
    state.outputColumns = outputColumns
    state.inputTableWidth = inputTableWidth
    state.outputTableWidth = outputTableWidth
  }

  const getSqlTransformOutputData = () => {
    let sqlQuery = refForm.value.getValues()?.query as string
    return sqlModelInfo(route.params.jobDefinitionCode as string, predecessorsNodeId, {
      "sourceFieldName": null,
      "query": sqlQuery
    }).then((res: any) => res.fields)
  }

  // Model indicates list click events.
  const onSwitchTable = (table: string) => {
    state.currentTable = table
    if (state.format === 'COMPATIBLE_DEBEZIUM_JSON') return

    (state.allTableData[0] as any).tableInfos.forEach(async (t: any) => {
      if (t.tableName === state.currentTable) {
        tempOutputTables = t.fields
        state.inputTableData = t.fields.filter((f: any) => !f.isSplit)
        if (state.columnSelectable) {
          if (state.optionsOutputTableData && (state.optionsOutputTableData[0] as any).tableName === state.currentTable) {
            // The default assignment of the source node output table structure.
            const result = state.optionsOutputTableData ? (state.optionsOutputTableData[0] as any).fields : tempOutputTables.filter((row: ModelRecord) =>
              state.selectedKeys.includes(row.name))
            state.outputTableData = result.filter((r: any) => !r.unSupport)
          } else {
            state.outputTableData = tempOutputTables.filter((t: any) => !t.unSupport)
          }
          // Assign default values to the optional input table structure of the source node.
          state.selectedKeys = state.outputTableData.filter((o: any) => !o.unSupport).map((o: any) => o.name)
        } else {
          if (transformType === 'FieldMapper') {
            // When the transform is empty, the data of the input model is directly copied to the output model.
            if (!state.secondTransformOptions && !state.transformOptions) {
              state.outputTableData = state.inputTableData.map((i: any) => {
                i.original_field = i.name
                return i
              })
              return false
            }

            const transformOptions = state.transformOptions.changeOrders ? state.transformOptions : state.secondTransformOptions
            state.outputTableData = state.optionsOutputTableData ?
              (state.optionsOutputTableData[0] as any).fields :
              t.fields.map((f: any, i: number) => {
                return {
                  ...f,
                  original_field: (transformOptions && Object.keys(transformOptions).length > 0) ?
                    transformOptions.changeOrders[i].sourceFieldName :
                    f.name
                }
              })
            state.outputTableData = state.outputTableData.map((o: any, i: number) => {
              if (!o.original_field) {
                o.original_field = transformOptions.changeOrders[i].sourceFieldName
              }
              return o
            })
            state.outputTableData.forEach((o: any) => {
              o.isError = o.original_field === state.schemaError.fieldName
            })
          } else if (transformType === 'MultiFieldSplit') {
            state.outputTableData = state.optionsOutputTableData ? (state.optionsOutputTableData[0] as any).fields.map((f: any) => {
              if (
                state.transformOptions.splits ||
                (
                  state.secondTransformOptions &&
                  state.secondTransformOptions.splits
                )
              ) {
                // When the data is echoed, it is judged whether the original
                // field has a split field, and if so, the split button of the
                // original field is disabled.
                const transformOptions = state.transformOptions.splits ? state.transformOptions : state.secondTransformOptions
                const needSplitDisabled = transformOptions.splits.map((t: any) => t.sourceFieldName)
                f.splitDisabled = needSplitDisabled.includes(f.name)

                // Add the split field to the original field value.
                transformOptions.splits.forEach((t: any) => {
                  if (t.outputFields.includes(f.name)) {
                    f.original_field = t.sourceFieldName
                    f.separator = t.separator
                    f.isSplit = true
                  }
                })

                if (!f.original_field) {
                  f.original_field = f.name
                }
              }

              return f
            }) : t.fields.map((f: any) => {
              return {
                ...f,
                original_field: f.name
              }
            })
          } else if (transformType === 'Copy') {
            state.outputTableData = state.optionsOutputTableData ?
              (state.optionsOutputTableData[0] as any).fields.map((f: any) => {
                if (
                  state.transformOptions.copyList ||
                  (
                    state.secondTransformOptions &&
                    state.secondTransformOptions.copyList
                  )
                ) {
                  const transformOptions = state.transformOptions.copyList ? state.transformOptions : state.secondTransformOptions
                  // Echo and judge the delete button of the copied data. and add the copied field to the original field value.
                  transformOptions.copyList.forEach((t: any) => {
                    const inputTableData = state.inputTableData.map((i: any) => i.name)

                    if (!inputTableData.includes(f.name)) {
                      f.copyTimes = -1
                      if (t.targetFieldName === f.name) {
                        f.original_field = t.sourceFieldName
                      } else {
                        // Request the data of the current node.
                        currentNodeId && queryTaskDetail(
                          route.params.jobDefinitionCode as string,
                          currentNodeId
                        ).then((res: any) => {
                          res.transformOptions && res.transformOptions.copyList.forEach((t: any) => {
                            if (t.targetFieldName === f.name) {
                              f.original_field = t.sourceFieldName
                            }
                          })
                        })
                      }
                    }
                  })

                  if (f.copyTimes !== -1) {
                    f.original_field = f.name
                  }
                }

                return f
              }) :
              t.fields.map((f: any) => {
                return {
                  ...f,
                  original_field: f.name
                }
              })
          } else if(transformType === 'Sql'){
            let table = await getSqlTransformOutputData()
            state.outputTableData = table
           
          } else {
            state.outputTableData = t.fields
          }
        }
      }
    })
    getColumns()
  }

  const onUpdatedCheckedRowKeys = (keys: any[]) => {
    state.selectedKeys = keys
    state.outputTableData = tempOutputTables.filter((row: ModelRecord) =>
      keys.includes(row.name)
    )
  }

  const onInit = (info: any) => {
    state.currentTable = info.tables.length ? info.tables[0] : ''
    state.tables = type === 'sink' ? [] : info.tables || []
    state.columnSelectable = info.columnSelectable || false
    state.database = info.database || ''
    state.datasourceInstanceId = info.datasourceInstanceId || ''
    state.format = info.format
    state.transformOptions = info.transformOptions
    // It is used to judge the output model of multiple continuously operable transform nodes.
    state.secondTransformOptions = info.transformOptions
    state.schemaError = schemaError

    if (
      (info.sceneMode && info.sceneMode === 'SINGLE_TABLE') ||
        transformType === 'FieldMapper' ||
        transformType === 'MultiFieldSplit' ||
        transformType === 'Copy'
    ) {
      state.optionsOutputTableData = info.outputSchema
    }

    // check debezium
    if (state.format === 'COMPATIBLE_DEBEZIUM_JSON') {
      state.inputTableData = [
        { name: 'topic', type: 'string' },
        { name: 'key', type: 'string' },
        { name: 'value', type: 'string' }
      ] as any
      state.outputTableData = [
        { name: 'topic', type: 'string' },
        { name: 'key', type: 'string' },
        { name: 'value', type: 'string' }
      ] as any
    } else {
      (type === 'transform' || state.datasourceInstanceId) &&
      (type === 'transform' || state.database) &&
      ((type === 'sink' || type === 'transform') || state.tables.length) &&
      getModelData()
    }

    getColumns()
  }

  watch(
    () => useI18n().locale,
    () => {
      getColumns()
    }
  )

  return {
    state,
    onSwitchTable,
    onUpdatedCheckedRowKeys,
    onInit
  }
}
