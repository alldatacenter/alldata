/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import { StringHelpers } from 'spline-utils'


export type SplineDataSourceInfo = {
    id: string
    name: string
    uri: string
    type: string
}


export type SplineDataSourceInfoDto = {
    source: string
    sourceType: string
}

export function toDataSourceInfo(entity: SplineDataSourceInfoDto): SplineDataSourceInfo {
    return {
        id: uriToDatsSourceId(entity.source),
        name: dataSourceUriToName(entity.source),
        uri: entity.source,
        type: entity.sourceType
    }
}

export function dataSourceUriToName(uri: string): string {
    return uri.split('/').slice(-1)[0]
}

export function dataSourceUriToType(uri: string): string {
    return uri.toLowerCase().endsWith('.csv') ? 'CSV' : 'Parquet'
}

export enum DataSourceWriteMode {
    Append = 'append',
    Overwrite = 'overwrite',
}

// dummy data helper
export function uriToDatsSourceId(uri: string): string {
    return StringHelpers.encodeBase64(uri)
}

export function idToDataSourceInfo(id: string): SplineDataSourceInfo {
    const uri = StringHelpers.decodeBase64(id)
    return uriToDatasourceInfo(uri)
}

export function uriToDatasourceInfo(uri: string, name?: string): SplineDataSourceInfo {
    return {
        id: uriToDatsSourceId(uri),
        name: name ?? dataSourceUriToName(uri),
        uri,
        type: dataSourceUriToType(uri)
    }
}
