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

import { axios } from '@/service/service'

export function querySyncTaskDefinitionPaging(params: any): any {
  return axios({
    url: '/job/definition',
    method: 'get',
    params
  })
}

export function createSyncTaskDefinition(data: any): any {
  return axios({
    url: '/job/definition',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function deleteSyncTaskDefinition(params: any): any {
  return axios({
    url: '/job/definition',
    method: 'delete',
    params
  })
}

export function getDefinitionNodesAndEdges(jobCode: string): any {
  return axios({
    url: `/job/${jobCode}`,
    method: 'get'
  })
}

export function getDefinitionDetail(jobCode: string): any {
  return axios({
    url: `/job/definition/${jobCode}`,
    method: 'get'
  })
}

export function getDefinitionConfig(jobCode: string): any {
  return axios({
    url: `/job/config/${jobCode}`,
    method: 'get'
  })
}

export function updateSyncTaskDefinition(jobCode: string, data: any): any {
  return axios({
    url: `/job/config/${jobCode}`,
    method: 'put',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function connectorSourcesTypeList(status = 'DOWNLOADED'): any {
  return axios({
    url: '/connector/sources',
    method: 'get',
    params: {
      status
    }
  })
}

export function connectorSinksTypeList(status = 'DOWNLOADED'): any {
  return axios({
    url: '/connector/sinks',
    method: 'get',
    params: {
      status
    }
  })
}

export function taskDefinitionForm(): any {
  return axios({
    url: '/job/env',
    method: 'get'
  })
}

export function saveTaskDefinitionDag(jobCode: string, data: any): any {
  return axios({
    url: `/job/dag/${jobCode}`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function deleteTaskDefinitionDag(
  jobCode: string,
  taskCode: string
): any {
  return axios({
    url: `/job/task/${jobCode}?pluginId=${taskCode}`,
    method: 'delete'
  })
}

export function saveTaskDefinitionItem(jobCode: string, data: any): any {
  return axios({
    url: `/job/task/${jobCode}`,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: () => JSON.stringify(data)
  })
}

export function queryTaskDetail(jobCode: string, taskCode: string): any {
  return axios({
    url: `/job/task/${jobCode}?pluginId=${taskCode}`,
    method: 'get'
  })
}

// source类型，获取源名称
export function listSourceName(
  jobId: string,
  sceneMode: string,
  status: 'DOWNLOADED' | 'NOT_DOWNLOAD' | 'ALL' = 'DOWNLOADED'
): any {
  return axios({
    url: '/datasource/sources',
    method: 'get',
    params: {
      jobId,
      sceneMode,
      status
    }
  })
}
// sink类型，获取源名称
export function findSink(
  jobId: string,
  status: 'DOWNLOADED' | 'NOT_DOWNLOAD' | 'ALL' = 'DOWNLOADED'
): any {
  return axios({
    url: '/datasource/sinks',
    method: 'get',
    params: {
      jobId,
      status
    }
  })
}

export function getFormStructureByDatasourceInstance(params: {
  connectorType: string
  connectorName?: string
  dataSourceInstanceId?: string
  jobCode: number
}): any {
  return axios({
    url: '/datasource/form',
    method: 'get',
    params
  })
}

export function connectorTransformsTypeList(jobId: string): any {
  return axios({
    url: '/datasource/transforms',
    method: 'get',
    params: {
      jobId
    }
  })
}

export function listEngine(): any {
  return axios({
    url: '/engine/list',
    method: 'get'
  })
}

export function listEngineType(): any {
  return axios({
    url: '/engine/type',
    method: 'get'
  })
}

export function getDatabaseByDatasource(datasourceName: string): any {
  return axios({
    url: '/datasource/databases',
    method: 'get',
    params: {
      datasourceName
    }
  })
}

export function getTableByDatabase(
  datasourceName: string,
  databaseName: string,
  filterName?: string,
  size?: number,
): any {
  size = size || 100
  filterName = filterName || ''
  return axios({
    url: '/datasource/tables',
    method: 'get',
    params: {
      datasourceName,
      databaseName,
      filterName,
      size
    }
  })
}

export function getInputTableSchema(
  datasourceId: string,
  databaseName: string,
  tableName: string
): any {
  return axios({
    url: '/datasource/schema',
    method: 'get',
    params: {
      datasourceId,
      databaseName,
      tableName
    }
  })
}

export function getOutputTableSchema(pluginName: string, data: any): any {
  return axios({
    url: `/job/table/schema?pluginName=${pluginName}`,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: () => JSON.stringify(data)
  })
}

export function getColumnProjection(pluginName: string): any {
  return axios({
    url: '/job/table/column-projection',
    method: 'get',
    params: {
      pluginName
    }
  })
}

export function checkDatabaseAndTable(
  datasourceId: string,
  data: { databases: Array<string>; tables: Array<string> }
): any {
  return axios({
    url: `/job/table/check?datasourceId=${datasourceId}`,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: () => JSON.stringify(data)
  })
}

export function modelInfo(datasourceId: string, data: any): any {
  return axios({
    url: `/datasource/schemas?datasourceId=${datasourceId}`,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: () => JSON.stringify(data)
  })
}

export function sqlModelInfo(taskId: string, pluginId: string, data: any): any {
  return axios({
    url: `/schema/derivation/sql?jobVersionId=${taskId}&inputPluginId=${pluginId}`,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: () => JSON.stringify(data)
  })
}
export function executeJob(jobDefineId: number): any {
  return axios({
    url: `/job/executor/execute?jobDefineId=${jobDefineId}`,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}