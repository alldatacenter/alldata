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


import { Params } from '@angular/router'
import { ProcessingStore, QuerySorter, RouterNavigation, SearchQuery, StringHelpers } from 'spline-utils'


export namespace SplineSearchDynamicTable {

    import SearchParams = SearchQuery.SearchParams


    export type State = {
        isInitialized: boolean
        sorting: QuerySorter.FieldSorter | null
        searchParams: SearchParams | null
        totalCount: number
        loadingProcessing: ProcessingStore.EventProcessingState
    }

    export function getDefaultState(): State {
        return {
            isInitialized: false,
            sorting: null,
            totalCount: 0,
            searchParams: null,
            loadingProcessing: ProcessingStore.getDefaultProcessingState(true)
        }
    }

    export function extractSearchParamsFromUrl(
        queryParams: Params,
        queryParamAlias: string): SearchParams | null {

        const urlString = queryParams[queryParamAlias]
        return urlString
            ? searchParamsFromUrlString(urlString)
            : null
    }

    export function applySearchParams(
        queryParams: Params,
        queryParamAlias: string,
        searchParams: SearchParams | null): Params {

        // keep some data in query params if it differs from the DS State
        const searchParamsString = searchParams !== null
            ? searchParamsToUrlString(searchParams)
            : null

        return RouterNavigation.setQueryParam(
            queryParams,
            queryParamAlias,
            searchParamsString,
        )
    }

    function searchParamsToUrlString(searchParams: SearchParams): string {
        return StringHelpers.encodeObjToUrlString(searchParams)
    }

    function searchParamsFromUrlString(searchParamsUrlString: string): SearchParams {
        return StringHelpers.decodeObjFromUrlString(searchParamsUrlString)
    }
}
