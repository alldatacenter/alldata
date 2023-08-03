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

export function queryRunningInstancePaging(params: any): any {
  return axios({
    url: '/job/metrics/detail',
    method: 'get',
    timeout: 60000,
    params
  })
}

export function querySyncTaskInstanceDetail(params: any): any {
  return axios({
    url: '/job/metrics/summary',
    method: 'get',
    timeout: 60000,
    params
  })
}

export function querySyncTaskInstanceDag(params: any): any {
  return axios({
    url: '/job/metrics/dag',
    method: 'get',
    timeout: 60000,
    params
  })
}

export function querySyncTaskInstancePaging(params: any): any {
  return axios({
    url: '/task/jobMetrics',
    method: 'get',
    params,
    timeout: 60000
  })
}

export function cleanStateByIds(taskInstanceIds: Array<any>) {
  return axios({
    url: 'ws/seaTunnel/batch-clean-task-instance-state',
    method: 'post',
    data: { taskInstanceIds }
  })
}

export function forcedSuccessByIds(taskInstanceIds: Array<any>) {
  return axios({
    url: 'ws/seaTunnel/batch-force-task-success',
    method: 'post',
    data: { taskInstanceIds }
  })
}

export function hanldlePauseJob(id: number): any {
  return axios({
    url: `/job/executor/pause?jobInstanceId=${id}`,
    method: 'get'
  })
}

export function hanldleRecoverJob(id: number): any {
  return axios({
    url: `/job/executor/restore?jobInstanceId=${id}`,
    method: 'get'
  })
}

export function hanldleDelJob(id: number): any {
  return axios({
    url: `/job/executor/del?jobInstanceId=${id}`,
    method: 'get'
  })
}
