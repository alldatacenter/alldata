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
import { AxiosRequestConfig } from 'axios'
import { IdReq, LogReq, TaskLogReq } from './types'

export function queryLog(params: LogReq): any {
  return axios({
    url: '/log/detail',
    method: 'get',
    params
  })
}

export function downloadTaskLog(params: IdReq): any {
  return axios({
    url: '/log/download-log',
    method: 'get',
    params
  })
}

export function queryTaskLog(params: TaskLogReq): any {
  return axios({
    url: '/ws/studio/taskLog',
    method: 'get',
    params,
    retry: 3,
    retryDelay: 1000
  } as AxiosRequestConfig)
}
