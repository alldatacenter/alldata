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

import { DataSourceWriteMode, ExecutionEventsQuery } from 'spline-api'
import { getDataSourceWriteModeInfoList, getDataSourceWriteModeLabel, SplineListBox } from 'spline-common'
import { DfControlDateRange, DfControlSelect } from 'spline-common/dynamic-filter/filter-controls'
import { DataSourceWithDynamicFilter } from 'spline-shared'


export namespace ExecutionEventsDynamicFilter {

    export enum FilterId {
        writeMode = 'writeMode',
        executedAtDataRange = 'executedAtDataRange'
    }

    export type Filter = {
        [FilterId.writeMode]: DataSourceWriteMode[]
        [FilterId.executedAtDataRange]: DfControlDateRange.Value
    }

    export function getExecutedAtFilterSchema(): DfControlDateRange.Schema<any> {
        return {
            id: FilterId.executedAtDataRange,
            type: DfControlDateRange.TYPE,
            label: 'SHARED.EXECUTION_EVENTS__DF__EXECUTED_AT'
        }
    }

    export function getWriteModeFilterSchema(): DfControlSelect.Schema<FilterId.writeMode> {
        return {
            id: FilterId.writeMode,
            type: DfControlSelect.TYPE,
            label: 'SHARED.EXECUTION_EVENTS__DF__WRITE_MODE',
            icon: 'save',
            records: getDataSourceWriteModeInfoList()
                .map(item => ({
                    label: item.label,
                    value: item.writeMode
                })),
            options: {
                dataMap: {
                    ...SplineListBox.getDefaultSimpleDataMap(),
                    valueToString: (value: DataSourceWriteMode) => getDataSourceWriteModeLabel(value)
                }
            }
        }
    }

    export function queryFilterToDynamicFilter(queryFilter: ExecutionEventsQuery.QueryFilter): Filter {
        return {
            [FilterId.writeMode]: queryFilter.writeMode ?? null,
            [FilterId.executedAtDataRange]: queryFilter.executedAtFrom && queryFilter.executedAtTo
                ? {
                    dateFrom: queryFilter.executedAtFrom,
                    dateTo: queryFilter.executedAtTo,
                }
                : null
        }
    }

    export function dynamicFilterToQueryFilter(
        filterValue: Filter,
        queryFilter: ExecutionEventsQuery.QueryFilter = {}): ExecutionEventsQuery.QueryFilter {

        if (filterValue[FilterId.executedAtDataRange]) {
            queryFilter = {
                ...queryFilter,
                executedAtFrom: filterValue[FilterId.executedAtDataRange].dateFrom,
                executedAtTo: filterValue[FilterId.executedAtDataRange].dateTo,
            }
        }
        // reset date filter
        else if (queryFilter.executedAtFrom) {
            queryFilter = {
                ...queryFilter,
                executedAtFrom: null,
                executedAtTo: null,
            }
        }

        if (filterValue[FilterId.writeMode] !== null) {
            queryFilter = {
                ...queryFilter,
                writeMode: filterValue[FilterId.writeMode],
            }
        }

        return queryFilter
    }

    export function getFilterMapping(): DataSourceWithDynamicFilter.FiltersMapping<ExecutionEventsQuery.QueryFilter, Filter> {
        return {
            dynamicFilterToQueryFilterMapFn: dynamicFilterToQueryFilter,
            queryFilterToDynamicFilterMapFn: queryFilterToDynamicFilter,
        }
    }


}
