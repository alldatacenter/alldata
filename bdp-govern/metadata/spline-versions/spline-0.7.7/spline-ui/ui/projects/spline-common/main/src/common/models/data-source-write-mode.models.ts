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

import { DataSourceWriteMode } from 'spline-api'


export type DataSourceWriteModeInfo = {
    writeMode: DataSourceWriteMode
    label: string
}

export function getDataSourceWriteModeLabel(writeMode: DataSourceWriteMode): string {
    const prefix = 'COMMON.DATA_SOURCE_WRITE_MODE'

    switch (writeMode) {
        case DataSourceWriteMode.Append:
            return `${prefix}.APPEND`

        case DataSourceWriteMode.Overwrite:
            return `${prefix}.OVERWRITE`

        default:
            console.warn(`Unknown write mode: ${writeMode}`)
            return writeMode as string
    }
}

export function getDataSourceWriteModeInfoList(): DataSourceWriteModeInfo[] {
    return Object.values(DataSourceWriteMode)
        .map(writeMode => ({
            writeMode,
            label: getDataSourceWriteModeLabel(writeMode)
        }))
}
