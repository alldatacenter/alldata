/*
 * Copyright 2020 ABSA Group Limited
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


import { Params } from '@angular/router'
import { ExecutionEventField, ExecutionEventsQuery } from 'spline-api'
import { RouterNavigation, SearchQuery } from 'spline-utils'


export namespace EventsListUrlState {

    export const URL_QUERY_PARAM__SEARCH_PARAMS = 'searchParams'

    export type State = SearchQuery.SearchParams<ExecutionEventsQuery.QueryFilter, ExecutionEventField>

    export function extractSearchParams(queryParams: Params): State | null {
        if (!queryParams[URL_QUERY_PARAM__SEARCH_PARAMS]) {
            return null
        }
        return decodeSearchParams(queryParams[URL_QUERY_PARAM__SEARCH_PARAMS])
    }

    export function applySearchParams(queryParams: Params, state: State | null): Params {
        return RouterNavigation.setQueryParam(
            queryParams,
            URL_QUERY_PARAM__SEARCH_PARAMS,
            state !== null ? encodeSearchParams(state) : null,
        )
    }

    export function encodeSearchParams(pagerState: State): string {
        return window.btoa(JSON.stringify(pagerState))
    }

    export function decodeSearchParams(pagerStateUrlString: string): State {
        const pagerState = JSON.parse(window.atob(pagerStateUrlString))
        return {
            ...pagerState,
            filter: {
                ...pagerState.filter,
                executedAtFrom: pagerState.filter.executedAtFrom ? new Date(pagerState.filter.executedAtFrom) : undefined,
                executedAtTo: pagerState.filter.executedAtTo ? new Date(pagerState.filter.executedAtTo) : undefined,
            },
        } as State
    }

}
