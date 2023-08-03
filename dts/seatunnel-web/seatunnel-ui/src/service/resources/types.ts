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

interface FileReq {
  file: any
}

interface ResourceTypeReq {
  type: 'FILE' | 'UDF' | 'GIT'
  programType?: string
}

interface ResourceTypesReq {
  types: string
}

interface UdfTypeReq {
  type: 'HIVE' | 'SPARK'
}

interface NameReq {
  name: string
}

interface FileNameReq {
  fileName: string
}

interface FullNameReq {
  fullName: string
}

interface IdReq {
  id: number
}

interface ContentReq {
  content: string
}

interface DescriptionReq {
  description?: string
}

interface CreateReq extends ResourceTypeReq, DescriptionReq {
  currentDir: string
  pid: number
}

interface UserIdReq {
  userId: number
}

interface OnlineCreateReq extends CreateReq, ContentReq {
  suffix: string
}

interface ProgramTypeReq {
  programType: 'JAVA' | 'SCALA' | 'PYTHON' | 'SQL'
}

interface ListReq {
  pageNo: number
  pageSize: number
  searchVal?: string | null
}

interface ViewResourceReq {
  limit: number
  skipLineNum: number
}

interface ResourceIdReq {
  resourceId: number
}

interface UdfFuncReq extends UdfTypeReq, DescriptionReq, ResourceIdReq {
  className: string
  funcName: string
  argTypes?: string
  database?: string
}

interface ResourceFile {
  id: number
  pid: number
  alias: string
  userId: number
  userName: string
  type: string
  directory: boolean
  fileName: string
  fullName: string
  description: string
  size: number
  updateTime: string
  path: string
  isEdit: boolean
  globalResource: boolean
  projectCode: number
  projectName: string
}

interface ResourceListRes {
  currentPage: number
  pageSize: number
  start: number
  totalPage: number
  totalList: ResourceFile[]
}

interface ResourceViewRes {
  alias: string
  content: string
}

interface GitReq {
  name?: string
  gitUrl?: string
  token?: string
  description?: string
  pid?: number
  id?: number
  gitProxy?: any
}

interface GitFile {
  id: number
  pid: number
  name: string
  path: string
  type: string
}

type accessTypeKey =
  | 'DATASOURCE'
  | 'WORKER_GROUP'
  | 'ALERT_GROUP'
  | 'ALERT_INSTANCE'
  | 'ENVIRONMENT'
  | 'TIMING'
  | 'CALENDAR'
  | 'DATA_CARD'
  | 'TENANT'
  | 'TASK_GROUP'
  | 'QUEUE'
  | 'FILE'
  | 'UDF_FILE'
  | 'UDF_FUNC'
  | 'GIT_FILE'
  | 'GIT_RESOURCE'

interface ResourceProjectReq {
  accessType: accessTypeKey
  accessCode: number
}

interface AuthResourceReq {
  globalResource: boolean
  accessType: accessTypeKey
  accessCode: number
  projectCodes: number[]
  isShare: boolean
}

interface ResourceListReq {
  accessType: accessTypeKey
  projectCode?: number
  resourceType?: string
}

export type FileType = 'FILE' | 'UDF'

export {
  FileReq,
  ResourceTypeReq,
  ResourceTypesReq,
  UdfTypeReq,
  NameReq,
  FileNameReq,
  FullNameReq,
  IdReq,
  ContentReq,
  DescriptionReq,
  CreateReq,
  UserIdReq,
  OnlineCreateReq,
  ProgramTypeReq,
  ListReq,
  ViewResourceReq,
  ResourceIdReq,
  UdfFuncReq,
  ResourceListRes,
  ResourceViewRes,
  ResourceFile,
  GitReq,
  GitFile,
  ResourceProjectReq,
  AuthResourceReq,
  ResourceListReq,
  accessTypeKey
}
