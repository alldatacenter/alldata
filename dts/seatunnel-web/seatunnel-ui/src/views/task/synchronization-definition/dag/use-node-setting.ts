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

import { reactive, ref, SetupContext } from 'vue'
import {
  getInputTableSchema,
  saveTaskDefinitionItem
} from '@/service/sync-task-definition'
import _, { omit } from 'lodash'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { ModelRecord } from './types'

export function useNodeSettingModal(
  props: any,
  ctx: SetupContext<('cancelModal' | 'confirmModal')[]>
) {
  const configurationFormRef = ref()
  const { t } = useI18n()
  const modelRef = ref()
  const route = useRoute()
  const state = reactive({
    nodeSettingModelFormRef: ref(),
    saving: false,
    loading: false,
    width: '60%',
    tab: 'configuration'
  })

  const formatParams = (values: any) => {
    const params = {
      pluginId: props.nodeInfo.pluginId,
      name: values.name,
      type: props.nodeInfo.type.toUpperCase(),
      connectorType:
        props.nodeInfo.type === 'transform'
          ? props.nodeInfo.connectorType
          : null
    } as { [key: string]: any }
    const config = omit(values, [
      'name',
      'datasourceInstanceId',
      'sceneMode',
      'kind',
      'kinds',
      'database',
      'tableName',
      'datasourceInstanceName',
      'pluginName',
      'datasourceName',
      'columnSelectable',
      'excludeKind',
      'includeKind'
    ]) as { [key: string]: string }

    if (values.kinds.length) {
      config[values.kind ? 'exclude_kinds' : 'include_kinds'] = JSON.stringify(
        values.kinds
      )
      config[!values.kind ? 'exclude_kinds' : 'include_kinds'] = ''
    }

    if (values.database || values.tableName) {
      params.tableOption = {
        databases: [] as string[],
        tables: [] as string[]
      }
      params.tableOption.databases =
        typeof values.database === 'string'
          ? [values.database]
          : Array.isArray(values.database)
          ? values.database
          : []
      params.tableOption.tables =
        typeof values.tableName === 'string'
          ? [values.tableName]
          : Array.isArray(values.tableName)
          ? values.tableName
          : []
    }

    if (modelRef.value) {
      params.selectTableFields = modelRef.value?.getSelectFields()
    }
    if (values.datasourceInstanceId) {
      params.dataSourceId = values.datasourceInstanceId
    }
    if (values.sceneMode) {
      params.sceneMode = values.sceneMode
    }
    params.config = JSON.stringify(config)
    return params
  }

  const formatOutputSchema = (data: any) => {
    return data[0].tableInfos.map((t: any) => ({
      fields: t.fields,
      tableName: t.tableName,
      database: data[0].database
    }))
  }

  const onSave = async (): Promise<boolean> => {
    if (state.saving) return false
    
    // Determine whether the current node type is Transform or Sink and there is no previous node.
    if (
      (props.nodeInfo.type === 'transform' || props.nodeInfo.type === 'sink') &&
      !props.nodeInfo.predecessorsNodeId
    ) {
      window.$message.warning(t('project.synchronization_definition.node_prev_check_tips'))
      return false
    }

    state.saving = true
    try {
      const validateResult = await configurationFormRef.value.validate()
      if (!validateResult) {
        handleTab('configuration')
        state.saving = false
        return false
      }

      const values = configurationFormRef.value.getValues()

      let modelOutputTableData
      if (props.nodeInfo.type === 'source') {
        const resultSchema = modelRef.value.getOutputSchema()
        // check debezium
        if (values.format && values.format === 'COMPATIBLE_DEBEZIUM_JSON') {
          modelOutputTableData = values.tableName.map((t: any) => {
            return {
              database: values.database,
              tableName: t,
              fields: [
                { name: 'topic', type: 'string' },
                { name: 'key', type: 'string' },
                { name: 'value', type: 'string' }
              ]
            }
          })
        } else {
          if (resultSchema.allTableData.length) {
            if (values.sceneMode === 'SINGLE_TABLE') {
              const result = resultSchema.allTableData
              if (resultSchema.outputTableData.length) {
                result[0].tableInfos[0].fields = resultSchema.outputTableData
              }
              modelOutputTableData = formatOutputSchema(result)
            } else {
              modelOutputTableData = formatOutputSchema(resultSchema.allTableData)
            }
          } else {
            window.$message.warning(t('project.synchronization_definition.check_model'), { duration: 0, closable: true })
            state.saving = false
            return false
          }
        }
      }

      if (props.nodeInfo.type === 'transform' && props.nodeInfo.predecessorsNodeId) {
        const resultSchema = modelRef.value.getOutputSchema()
        if (resultSchema.allTableData.length) {
          if (
            props.nodeInfo.connectorType === 'FieldMapper' ||
            props.nodeInfo.connectorType === 'MultiFieldSplit' ||
            props.nodeInfo.connectorType === 'Copy'
          ) {
            const result = resultSchema.allTableData
            if (resultSchema.outputTableData.length) {
              result[0].tableInfos[0].fields = resultSchema.outputTableData
            }
            modelOutputTableData = formatOutputSchema(result)
          } else {
            modelOutputTableData = formatOutputSchema(resultSchema.allTableData)
          }
        } else {
          window.$message.warning(t('project.synchronization_definition.check_model'), { duration: 0, closable: true })
          state.saving = false
          return false
        }
      }

      const transformOptions: any = {}

      if (props.nodeInfo.connectorType === 'FieldMapper') {
        const resultSchema = modelRef.value.getOutputSchema()
        transformOptions.changeOrders = resultSchema.outputTableData.map((o: any, i: number) => {
          return {
            sourceFieldName: o.original_field,
            index: i
          }
        })
        transformOptions.renameFields = resultSchema.outputTableData.filter((o: any) => o.name !== o.original_field).map((o: any) => ({
          sourceFieldName: o.original_field,
          targetName: o.name
        }))
        const outputTableDataNames = resultSchema.outputTableData.map((o: any) => ({sourceFieldName: o.original_field}))
        const inputTableDataNames = resultSchema.inputTableData.map((o: any) => ({sourceFieldName: o.name}))
        transformOptions.deleteFields = _.xorWith(outputTableDataNames, inputTableDataNames, _.isEqual)
      } else if (props.nodeInfo.connectorType === 'MultiFieldSplit') {
        const resultSchema = modelRef.value.getOutputSchema()
        const hasSeparator = resultSchema.outputTableData.filter((o: any) => o.separator)
        transformOptions.splits = _.uniqWith(hasSeparator.map((o: any) => {
          return {
            sourceFieldName: o.original_field,
            separator: o.separator,
            outputFields: _.groupBy(hasSeparator, 'separator')[o.separator].map((h: any) => h.name)
          }
        }), _.isEqual)
      } else if (props.nodeInfo.connectorType === 'Copy') {
        const resultSchema = modelRef.value.getOutputSchema()
        const hasCopyColumn = resultSchema.outputTableData.filter((o: any) => o.copyTimes === -1)
        transformOptions.copyList = hasCopyColumn.map((h: any) => ({
          sourceFieldName: h.original_field,
          targetFieldName: h.name
        }))
      } else if(props.nodeInfo.connectorType === 'Sql') {
        const resultSchema = modelRef.value.getOutputSchema()
        const tableInfo = resultSchema.allTableData[0].tableInfos
        transformOptions.sql = {
          "sourceFieldName": resultSchema.outputTableData[0]?.name || null,
          "query": values.query
        }
        modelOutputTableData = [{
          database: tableInfo[0].database,
          tableName: tableInfo[0].tableName,
          fields: resultSchema.outputTableData
        }]
      }

      await saveTaskDefinitionItem(
        route.params.jobDefinitionCode as string,
        {
          ...formatParams(values),
          outputSchema: modelOutputTableData,
          transformOptions
        }
      )

      ctx.emit(
        'confirmModal',
        {
          ...formatParams(values),
          outputSchema: modelOutputTableData,
          transformOptions
        }
      )

      state.saving = false
      state.tab = 'configuration'
      state.width = '60%'
      return true
    } catch (err) {
      state.saving = false
      return false
    }
  }

  const initModelData = (node: any) => {
    const obj: any = {
      tables: node.tableName
        ? typeof node.tableName === 'string'
          ? [node.tableName]
          : node.tableName
        : [],
      database: node.database
        ? typeof node.database === 'string'
          ? node.database
          : node.database[0]
        : [],
      datasourceName: node.datasourceName,
      datasourceInstanceId: node.datasourceInstanceId,
      columnSelectable: node.columnSelectable,
      outputSchema: props.nodeInfo.outputSchema,
      sceneMode: node.sceneMode ? node.sceneMode : '',
      transformOptions: props.nodeInfo.transformOptions
    }

    if (node.format && node.format === 'COMPATIBLE_DEBEZIUM_JSON') {
      obj.format = node.format
    } else {
      obj.format = 'DEFAULT'
    }

    modelRef.value.initData(obj)
  }

  const handleChangeTable = async (node: any) => {
    if (!modelRef.value) return
    const currentTable = _.isArray(node.tableName)
      ? node.tableName[0]
      : node.tableName

    const currentDatabase = _.isArray(node.database)
      ? node.database[0]
      : node.database

    if (!currentTable) return

    const result = await getInputTableSchema(
      node.datasourceInstanceId,
      currentDatabase,
      currentTable
    )

    const selectedKeys = result.map((row: ModelRecord) => row.name)
    modelRef.value.setSelectFields(selectedKeys)
  }

  const handleTab = (tab: 'configuration' | 'model') => {
    state.width = tab === 'configuration' ? '60%' : '80%'
    state.tab = tab
    if (tab === 'model' && modelRef.value) {
      initModelData(configurationFormRef.value.getValues())
    }
  }

  return {
    state,
    configurationFormRef,
    modelRef,
    onSave,
    handleTab,
    handleChangeTable
  }
}
