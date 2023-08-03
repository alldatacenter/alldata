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

export type NodeType = 'source' | 'sink' | 'transform'
export type OptionType = 'datasource' | 'database' | 'table'
export type { TableColumns } from 'naive-ui/es/data-table/src/interface'
export interface ModelRecord {
  format: string
  comment: string
  defaultValue: string
  nullable: boolean
  primaryKey: boolean
  type: string
  name: string
  isEdit?: boolean
  isSplit?: boolean
  copyTimes?: number
  separator?: string
  original_field?: string
  splitDisabled?: boolean
  unSupport?: boolean
  outputDataType?: string
}
export type JobType = 'DATA_REPLICA' | 'DATA_INTEGRATION'
export type SceneMode = 'MULTIPLE_TABLE' | 'SPLIT_TABLE' | 'SINGLE_TABLE'
export type InputEdge = {
  inputPluginId: string
  targetPluginId: string
}
export type InputPlugin = {
  config: string
  connectorType: string
  dataSourceId: 0
  name: string
  pluginId: string
  sceneMode: SceneMode
  selectTableFields: {
    all: boolean
    tableFields: string[]
  }
  tableOption: {
    databases: string[]
    tables: string[]
  }
  type: NodeType
}
export type NodeInfo = { [key: string]: any }
