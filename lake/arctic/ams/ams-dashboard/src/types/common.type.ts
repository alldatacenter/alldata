/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

export interface IOption {
  [propName: string]: any;
}
export interface IHttpResponse {
  code: number;
  msg: string;
  result?: IOption;
  data?: IOption;
}
export interface UserInfo {
  userName: string
  token?: string
}

export interface IColumns {
  title: string
  dataIndex: string
  key?: string
  ellipsis?: boolean
  width?: number | string
  scopedSlots?: any
  children?: IColumns[]
}

export interface IOptions {
  label: string
  value: string
  [propName: string]: string
}

export interface ILableAndValue {
  label: string
  value: string
}
export interface IMap<T> {
  [key: string]: T;
}

export interface IKeyAndValue {
  key: string
  value: string
}
export interface IBaseDetailInfo {
  tableType: string
  tableName: string
  createTime: string
  size: string
  file: string
  averageFile: string
  tableFormat: string
  hasPartition: boolean
}

export interface DetailColumnItem {
  field: string
  type: string
  comment: string
}

export interface IField {
  field: string
  type: string
  description: string
}

export interface PartitionColumnItem {
  field: string
  sourceField: string
  transform: string
}

export interface IDetailsInfo {
  pkList: DetailColumnItem[],
  partitionColumnList: PartitionColumnItem[],
  properties: IMap<string>[],
  metrics: IMap<string | number>[],
  schema: DetailColumnItem[],
}
export interface ICompMap {
  Details: string
  Partitions: string
  Transactions: string
  Operations: string
  Optimizes: string
}
export interface TableBasicInfo {
  catalog: string
  database: string
  tableName: string
}

export interface PartitionItem {
  partition: string
  fileCount: number
  size: string
  lastCommitTime: number | string
}

export interface BreadcrumbPartitionItem {
  file: string
  fsn: number
  fileType: string
  size: string
  commitTime: number | string
  commitId: string
  path: string
}

export interface BreadcrumbTransactionItem {
  file: string
  fsn: number
  partition: string
  fileType: string
  size: string
  commitTime: number | string
}

export interface TransactionItem {
  transactionId: string
  fileCount: number
  fileSize: string
  commitTime: string
  snapshotId: string
}

export interface OperationItem {
  ts: number | string
  operation: string
}

export interface IHistoryPathInfoItem {
  path: string
  query: IOption
}

export interface GlobalState {
  userInfo: UserInfo
  isShowTablesMenu: boolean
  historyPathInfo: IHistoryPathInfoItem
}

export interface ICatalogItem {
  catalogName: string
  catalogType: string
}
export interface IDebugResult {
  status: number;
  columns: string[];
  rowData: (string | null)[][];
  id: string
}

export interface IGroupItem {
  optimizerGroupId: number
  optimizerGroupName: string
  label: string
  value: string
}

export interface IGroupItemInfo {
  occupationCore: number
  occupationMemory: number | string
  unit: string
}

export interface ITableIdentifier {
  catalog: string
  database: string
  tableName: string
  id: number
}
export interface IOptimizeTableItem {
  tableName: string
  optimizeStatus: string
  fileCount: number
  fileSize: number
  quota: number
  quotaOccupation: number
  quotaOccupationDesc: string
  duration: number
  durationDesc: string
  fileSizeDesc: string
  tableIdentifier: ITableIdentifier
  tableNameOnly?: string
}

export interface IOptimizeResourceTableItem {
  index: number
  jobId: number
  jobStatus: string
  coreNumber: number
  memory: number
  jobmanagerUrl: string
  parallelism: number
  jobType: string
  groupName: string
  resourceAllocation: string
}

export interface IOverviewSummary {
  catalogCnt: number
  tableCnt: number
  tableTotalSize: number
  totalCpu: string
  totalMemory: number
  displayTotalSize?: string
  displayTotalMemory?: string
  totalSizeUnit?: string
  totalMemoryUnit?: string
}
export interface ITimeInfo {
  yTitle: string;
  colors: string[];
  name: string[];
}

export interface IChartLineData {
  timeLine: string[]
  data1: number[] | string[]
  data2: number[] | string[]
}

export interface ITopTableItem {
  tableName: string
  size: number
  fileCnt: number
  displaySize: string
  tableNameOnly: string
  index: number
}

export interface IContainerSetting {
  name: string
  type: string
  properties: IMap<string>,
  optimizeGroup: IMap<string>[],
  propertiesArray?: []
}

export interface IResourceUsage {
  timeLine: string[]
  usedCpu: string[]
  usedCpuDivision: string[]
  usedCpuPercent: string[]
  usedMem: string[]
  usedMemDivision: string[]
  usedMemPercent: string[]
}

export enum debugResultBgcMap {
  Created = '#f5f5f5',
  Failed = '#fff2f0',
  Finished = '#f6ffed',
  Canceled = '#f5f5f5'
}

export enum upgradeStatusMap {
  failed = 'FAILED',
  upgrading = 'UPGRADING',
  success = 'SUCCESS',
  none = 'NONE' // can upgrade
}

export enum tableTypeIconMap {
  ICEBERG = 'iceberg',
  ARCTIC = 'arctic',
  HIVE = 'hive'
}
