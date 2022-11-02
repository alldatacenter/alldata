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

import { ExecutionEvent, ExecutionEventsQuery } from 'spline-api'
import { DynamicFilterSchema } from 'spline-common/dynamic-filter'
import { DfControlDateRange } from 'spline-common/dynamic-filter/filter-controls'
import { DataSourceWithDynamicFilter, ExecutionEventsDynamicFilter } from 'spline-shared'


export namespace DsOverviewHistoryPage {

    export type State = {
        selectedExecutionEvent: ExecutionEvent | null
    }

    export function getDefaultState(): State {
        return {
            selectedExecutionEvent: null
        }
    }

    export function reduceSelectDsState(state: State, value: ExecutionEvent | null): State {
        return {
            ...state,
            selectedExecutionEvent: value
        }
    }

    export type Filter = ExecutionEventsDynamicFilter.Filter

    export type FilterId = keyof Filter
    export const FilterId = { ...ExecutionEventsDynamicFilter.FilterId }

    export function getDynamicFilterSchema(): DynamicFilterSchema<Filter> {
        return [
            {
                ...ExecutionEventsDynamicFilter.getExecutedAtFilterSchema(),
                label: 'DATA_SOURCES.DS_STATE_HISTORY__FILTER__CREATED_AT'
            } as DfControlDateRange.Schema<FilterId>,
            ExecutionEventsDynamicFilter.getWriteModeFilterSchema()
        ]
    }

    export function getFiltersMapping(): DataSourceWithDynamicFilter.FiltersMapping<ExecutionEventsQuery.QueryFilter, Filter> {
        return ExecutionEventsDynamicFilter.getFilterMapping()
    }

}
