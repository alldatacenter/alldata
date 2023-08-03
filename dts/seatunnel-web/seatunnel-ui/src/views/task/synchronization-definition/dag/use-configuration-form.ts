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

import { reactive } from 'vue'
import _, { cloneDeep, find, omit } from 'lodash'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useFormField } from '@/components/dynamic-form/use-form-field'
import { useFormRequest } from '@/components/dynamic-form/use-form-request'
import { useFormValidate } from '@/components/dynamic-form/use-form-validate'
import { useFormStructure } from '@/components/dynamic-form/use-form-structure'
import {
  listSourceName,
  getDatabaseByDatasource,
  getTableByDatabase,
  getFormStructureByDatasourceInstance,
  getColumnProjection,
  findSink
} from '@/service/sync-task-definition'
import { useSynchronizationDefinitionStore } from '@/store/synchronization-definition'
import type { NodeType } from './types'

export const useConfigurationForm = (
  nodeType: NodeType,
  transformType: string
) => {
  const { t } = useI18n()
  const dagStore = useSynchronizationDefinitionStore()
  const route = useRoute()
  const initialModel = {
    name: '',
    datasourceInstanceId: null,
    datasourceInstanceName: null,
    sceneMode: null,
    database: null as null | string | string[],
    tableName: null as null | string | string[],
    kinds: [],
    kind: 0,
    columnSelectable: false,
    pluginName: '',
    datasourceName: '',
    query: ''
  }

  const state = reactive({
    model: cloneDeep(initialModel),
    loading: false,
    datasourceOptions: [],
    datasourceLoading: false,
    databaseOptions: [],
    databaseLoading: false,
    tableOptions: [],
    tableLoading: false,
    formStructure: [],
    formLocales: {},
    formName: '',
    formLoading: false,
    inputTableData: [],
    outputTableData: [],
    tableColumnsLoading: false,
    rules: {
      name: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (ignore: any, value: string) => {
          if (!value) {
            return new Error(
              t('project.synchronization_definition.node_name_validate')
            )
          }
        }
      },
      datasourceInstanceId: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (ignore: any, value: string) => {
          if (!value) {
            return new Error(
              t('project.synchronization_definition.source_name_validate')
            )
          }
        }
      },
      sceneMode: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (ignore: any, value: string) => {
          if (!value) {
            return new Error(
              t('project.synchronization_definition.scene_mode_validate')
            )
          }
        }
      },
      database: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (ignore: any, value: string) => {
          if (!value) {
            return new Error(
              t('project.synchronization_definition.database_validate')
            )
          }
        }
      },
      tableName: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (ignore: any, value: string) => {
          if (!value) {
            return new Error(
              t('project.synchronization_definition.table_name_validate')
            )
          }
        }
      },
      kinds: {
        required: true,
        trigger: ['input', 'blur'],
        validator: () => {
          if (state.model.kinds.length === 0) {
            return new Error(
              state.model.kind
                ? t('project.synchronization_definition.exclude_kind_validate')
                : t('project.synchronization_definition.include_kind_validate')
            )
          }
        }
      },
      query: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (ignore: any, value: string) => {
          if (!value) {
            return new Error(
              t('project.synchronization_definition.query_validate')
            )
          }
        }
      },
    }
  })

  const getDatasourceOptions = async (sceneMode: string) => {
    if (state.datasourceLoading) return
    state.datasourceLoading = true
    try {
      const result = await listSourceName(
        route.params.jobDefinitionCode as string,
        sceneMode
      )
      state.datasourceOptions = result.map((item: any) => ({
        label: item.dataSourceInstanceName,
        value: item.dataSourceInstanceId,
        pluginName:
          item.dataSourceInfo?.connectorInfo?.pluginIdentifier.pluginName,
        datasourceName: item.dataSourceInfo?.datasourceName
      }))
    } finally {
      state.datasourceLoading = false
    }
  }

  const getDatabaseOptions = async (
    datasourceInstanceId: string,
    option?: any
  ) => {
    if (option?.label) state.model.datasourceInstanceName = option.label
    if (option?.datasourceName) {
      state.model.datasourceName = option.datasourceName
      getColumnSelectable(option.datasourceName)
    }
    if (option?.pluginName) state.model.pluginName = option.pluginName
    if (state.databaseLoading) return
    state.databaseLoading = true
    try {
      if (nodeType !== 'transform') {
        const result = await getDatabaseByDatasource(option.label)
        state.databaseOptions = result.map((item: string) => ({
          label: item,
          value: item
        }))
      }
      await getFormStructure(datasourceInstanceId)
    } finally {
      state.databaseLoading = false
    }
  }

  const getTableOptions = async (databases: Array<string> | string, filterName?: string, size?: number) => {
    filterName = filterName || ''
    if (
      nodeType === 'source' ||
      (dagStore.getDagInfo.jobType === 'DATA_INTEGRATION' &&
        nodeType === 'sink')
    ) {
      if (state.tableLoading) return
      if (_.isArray(databases) && databases.length === 0) {
        state.tableOptions = []
        return
      }
      state.tableLoading = true
      try {
        const result = await getTableByDatabase(
          state.model.datasourceInstanceName || '',
          typeof databases === 'string' ? databases : databases[0],
          filterName,
          size
        )
        state.tableOptions = result.map((item: any) => ({
          label: item,
          value: item
        }))
      } finally {
        state.tableLoading = false
      }
      return
    }
  }

  const getSinks = async () => {
    try {
      if (state.datasourceLoading) return
      state.datasourceLoading = true
      const result = await findSink(route.params.jobDefinitionCode as string)
      state.datasourceOptions = result.map((item: any) => ({
        label: item.dataSourceInstanceName,
        value: item.dataSourceInstanceId,
        pluginName:
          item.dataSourceInfo?.connectorInfo?.pluginIdentifier.pluginName,
        datasourceName: item.dataSourceInfo?.datasourceName
      }))
    } finally {
      state.datasourceLoading = false
    }
  }

  const getFormStructure = async (datasourceInstanceId = '') => {
    if (state.formLoading) return
    state.formLoading = true
    try {
      const params = {
        jobCode: Number(route.params.jobDefinitionCode) as number,
        connectorType: nodeType
      } as {
        connectorType: string
        connectorName?: string
        dataSourceInstanceId?: string
        jobCode: number
      }
      if (nodeType === 'transform') params.connectorName = transformType
      if (nodeType !== 'transform')
        params.dataSourceInstanceId = datasourceInstanceId
      const resJson = await getFormStructureByDatasourceInstance(params)
      if (resJson === 'null') return
      const res = JSON.parse(resJson)
      state.formName = res.name
      res.forms = res.forms.filter(
        (form: { field: string }) =>
          !['exclude_kinds', 'include_kinds'].includes(form.field)
      )
      state.formLocales = res.locales
      Object.assign(state.model, useFormField(res.forms))
      Object.assign(state.rules, useFormValidate(res.forms, state.model, t))
      state.formStructure = useFormStructure(
        res.apis ? useFormRequest(res.apis, res.forms) : res.forms
      ) as any
    } finally {
      state.formLoading = false
    }
  }

  const getColumnSelectable = async (pluginName: string) => {
    if (state.model.sceneMode !== 'SINGLE_TABLE') {
      state.model.columnSelectable = false
      return
    }
    if (dagStore.getColumnSelectable(pluginName) !== undefined) {
      state.model.columnSelectable = dagStore.getColumnSelectable(pluginName)
      return
    }
    const res = await getColumnProjection(pluginName)
    state.model.columnSelectable = res
    dagStore.setColumnSelectable(pluginName, res)
  }

  const updateFormValues = async (values: any) => {
    if (state.loading) return
    state.loading = true
    try {
      state.model.datasourceInstanceId = values.dataSourceId
      state.model.name = values.name
      state.model.sceneMode = values.sceneMode
      if (values.sceneMode === 'SPLIT_TABLE') {
        state.model.database = values.tableOption?.databases || []
      } else {
        state.model.database = values.tableOption?.databases?.length
          ? values.tableOption.databases[0]
          : null
      }

      if (values.sceneMode) {
        await getDatasourceOptions(values.sceneMode)
      }
      if (nodeType === 'sink') {
        await getSinks()
      }

      if (values.dataSourceId) {
        const option = find(
          state.datasourceOptions,
          (item: { value: string }) => item.value === values.dataSourceId
        )
        await getDatabaseOptions(values.dataSourceId, option)
      }
      if (nodeType === 'transform') {
        await getFormStructure()
      }

      if (values.tableOption?.databases?.length) {
        await getTableOptions(values.tableOption.databases[0], '', state.model.sceneMode === 'MULTIPLE_TABLE' ? 9999999 : 100)
      }

      if (values.sceneMode === 'MULTIPLE_TABLE') {
        state.model.tableName = values.tableOption?.tables || []
      } else {
        state.model.tableName = values.tableOption?.tables.length
          ? values.tableOption.tables[0]
          : null
      }

      if (values.config) {
        const config = JSON.parse(values.config)
        Object.assign(
          state.model,
          omit(config, ['exclude_kinds', 'include_kinds'])
        )
        if (config.exclude_kinds || config.include_kinds) {
          state.model.kind = config.exclude_kinds ? 1 : 0
          state.model.kinds = JSON.parse(
            config.exclude_kinds || config.include_kinds
          )
        }
      }
    } finally {
      state.loading = false
    }
  }

  return {
    state,
    dagStore,
    getDatasourceOptions,
    getDatabaseOptions,
    getTableOptions,
    getFormStructure,
    updateFormValues,
    getSinks
  }
}

export const getSceneModeOptions = (jobType: string, t: Function) => {
  return [
    {
      label: t('project.synchronization_definition.multi_table_sync'),
      value: 'MULTIPLE_TABLE',
      disabled: jobType === 'DATA_INTEGRATION'
    },
    {
      label: t('project.synchronization_definition.sub_library_and_sub_table'),
      value: 'SPLIT_TABLE',
      disabled: jobType === 'DATA_REPLICA'
    },
    {
      label: t('project.synchronization_definition.single_table_sync'),
      value: 'SINGLE_TABLE',
      disabled: jobType === 'DATA_REPLICA'
    }
  ]
}
