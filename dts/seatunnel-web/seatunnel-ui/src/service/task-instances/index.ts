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
import utils from '@/utils'
import { ProjectCodeReq, IdReq, TaskListReq } from './types'

export function queryTaskListPaging(params: TaskListReq): any {
  return axios({
    url: '/projects/task-instances',
    method: 'get',
    params
  })
}

export function forceSuccess(taskId: IdReq, projectCode: ProjectCodeReq): any {
  return axios({
    url: `/projects//task-instances/${taskId.id}/force-success`,
    method: 'post'
  })
}

export function downloadLog(id: number): void {
  utils.downloadFile('log/download-log', { taskInstanceId: id })
}

export function cleanState(
  projectCode: number,
  taskInstanceIds: number[],
  cleanDownstream?: boolean
): any {
  return axios({
    url: '/projects/task-instances/clean-task-instance-state',
    method: 'post',
    data: { taskInstanceIds, cleanDownstream },
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function checkDependentChain(
  projectCode: number,
  taskInstanceIds: number[]
): any {
  return axios({
    url: `/ws/projects/${projectCode}/task-instances/dependent-chain-rerun-precheck`,
    method: 'post',
    data: { taskInstanceIds },
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    timeout: 60000,
    transformRequest: (params) => JSON.stringify(params)
  })
}

export function dependentChainRerun(
  projectCode: number,
  taskInstanceIds: number[]
): any {
  return axios({
    url: `/ws/projects/${projectCode}/task-instances/dependent-chain-rerun`,
    method: 'post',
    data: { taskInstanceIds },
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    timeout: 60000,
    transformRequest: (params) => JSON.stringify(params)
  })
}
