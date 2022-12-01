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

import { DtLayoutBuilder, DynamicTableColumnSchema, DynamicTableDataMap, isDtColVisible } from '../../models'


export namespace DynamicTableStore {

    export type State = {
        visibleColumnsIds: string[]
        dataMap: DynamicTableDataMap
    }

    export function getDefaultState(): State {
        return {
            visibleColumnsIds: [],
            dataMap: null
        }
    }

    export function reduceDataMap(state: State, dataMap: DynamicTableDataMap): State {
        return {
            ...state,
            dataMap: decorateDataMap(dataMap),
            visibleColumnsIds: dataMap
                .filter(col => isDtColVisible(col))
                .map(col => col.id)
        }
    }

    export function decorateDataMap(dataMap: DynamicTableDataMap): DynamicTableDataMap {
        return dataMap
            .map(item => ({
                ...decorateWidth(item),
            }))
    }

    export function decorateWidth(columnMap: DynamicTableColumnSchema): DynamicTableColumnSchema {
        if (!columnMap.width) {
            return columnMap
        }

        return {
            ...columnMap,
            layout: {
                ...DtLayoutBuilder.setWidth(columnMap.width, (columnMap.layout || {}))
            }

        }
    }

}
