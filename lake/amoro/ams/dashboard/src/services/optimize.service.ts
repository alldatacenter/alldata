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

import request from '@/utils/request'

export function getOptimizerGroups() {
  return request.get('ams/v1/optimize/optimizerGroups')
}

export function getOptimizerTableList(
  params: {
    optimizerGroup: string
    page: number
    pageSize: number
  }
) {
  const { optimizerGroup, page, pageSize } = params
  return request.get(`ams/v1/optimize/optimizerGroups/${optimizerGroup}/tables`, { params: { page, pageSize } })
}

export function getOptimizerResourceList(
  params: {
    optimizerGroup: string
    page: number
    pageSize: number
  }
) {
  const { optimizerGroup, page, pageSize } = params
  return request.get(`ams/v1/optimize/optimizerGroups/${optimizerGroup}/optimizers`, { params: { page, pageSize } })
}

export function getQueueResourceInfo(
  optimizerGroup: string
) {
  return request.get(`ams/v1/optimize/optimizerGroups/${optimizerGroup}/info`)
}

export function scaleoutResource(
  params: {
    optimizerGroup: string
    parallelism: number
  }
) {
  const { optimizerGroup, parallelism } = params
  return request.post(`ams/v1/optimize/optimizerGroups/${optimizerGroup}/optimizers`, { parallelism })
}

export function releaseResource(
  params: {
    optimizerGroup: string
    jobId: number
  }
) {
  const { optimizerGroup, jobId } = params
  return request.delete(`ams/v1/optimize/optimizerGroups/${optimizerGroup}/optimizers/${jobId}`)
}

export async function getResourceGroupsListAPI() {
  const result = await request.get('ams/v1/optimize/resourceGroups')
  return result
}

export const getGroupContainerListAPI = async() => {
  const result = await request.get('ams/v1/optimize/containers/get')
  return result
}

export const addResourceGroupsAPI = (params: {name: string; container: string; properties: {[prop: string]: string}}) => {
  return request.post('ams/v1/optimize/resourceGroups', params)
}

export const updateResourceGroupsAPI = (params: {name: string; container: string; properties: {[prop: string]: string}}) => {
  return request.put('ams/v1/optimize/resourceGroups', params)
}

export const groupDeleteCheckAPI = (params: {name: string}) => {
  return request.get(`/ams/v1/optimize/resourceGroups/${params.name}/delete/check`)
}

export const groupDeleteAPI = (params: {name: string}) => {
  return request.delete(`/ams/v1/optimize/resourceGroups/${params.name}`)
}
