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
import { SplineColors, SplineIcon } from 'spline-common'
import {
    DtCellIcon,
    DtCellLayout,
    DtLayoutBuilder,
    DtLayoutSize,
    DynamicTableColumnSchema,
    TCellValueFn
} from 'spline-common/dynamic-table'


export namespace SplineDataSourceSharedDtSchema {

    export function getWriteModeDefaultLayout(): DtCellLayout {
        return (new DtLayoutBuilder())
            .alignCenter()
            .setWidth('50px')
            .visibleAfter(DtLayoutSize.sm)
            .toLayout()
    }


    export function getWriteModeColSchema(
        getWriteModeFn: TCellValueFn<DataSourceWriteMode>
    ): Partial<DynamicTableColumnSchema<DtCellIcon.Icon, unknown, unknown, DtCellIcon.Options>> {

        return {
            type: DtCellIcon.TYPE,
            value: (rowData) => {
                switch (getWriteModeFn(rowData)) {
                    case DataSourceWriteMode.Append:
                        return 'post_add'
                    case DataSourceWriteMode.Overwrite:
                        return 'save'
                    default:
                        return null
                }
            },
            options: (rowData) => {
                const writeMode = getWriteModeFn(rowData)
                const color: SplineIcon.Color = writeMode === DataSourceWriteMode.Append
                    ? SplineColors.BLUE
                    : SplineColors.GREEN
                const hint: string = writeMode === DataSourceWriteMode.Append
                    ? 'SHARED.DYNAMIC_TABLE.DS_WRITE_MODE__APPEND'
                    : 'SHARED.DYNAMIC_TABLE.DS_WRITE_MODE__OVERWRITE'

                return {
                    color,
                    hint
                }
            },
            layout: getWriteModeDefaultLayout(),
            isSortable: false
        }
    }
}
