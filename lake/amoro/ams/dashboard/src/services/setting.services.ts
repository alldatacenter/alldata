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

import { IMap } from '@/types/common.type'
import request from '@/utils/request'

export function getCatalogsTypes() {
  return request.get('ams/v1/catalog/metastore/types')
}
export function getCatalogsSetting(catalogName: string) {
  return request.get(`ams/v1/catalogs/${catalogName}`)
}
export function delCatalog(catalogName: string) {
  return request.delete(`ams/v1/catalogs/${catalogName}`)
}
export function checkCatalogStatus(catalogName: string) {
  return request.get(`ams/v1/catalogs/${catalogName}/delete/check`)
}
export function saveCatalogsSetting(params: {
  name: string
  type: string
  storageConfig: IMap<string>
  authConfig: IMap<string>
  properties: IMap<string>
  isCreate?: boolean
}) {
  const { isCreate, name } = params
  delete params.isCreate
  if (isCreate) {
    return request.post('ams/v1/catalogs', { ...params })
  }
  return request.put(`ams/v1/catalogs/${name}`, { ...params })
}
export function getSystemSetting() {
  return request.get('ams/v1/settings/system')
}
export function getContainersSetting() {
  return request.get('ams/v1/settings/containers')
}
