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

import { MatSortable } from '@angular/material/sort'
import { Sort } from '@angular/material/sort/sort'

import { ProcessingStore } from '../../../store'
import { SplineRecord } from '../heplers'
import { DEFAULT_PAGER, QueryPager, QuerySorter } from '../query'


export namespace SearchQuery {

    import FieldSorter = QuerySorter.FieldSorter
    import SortDir = QuerySorter.SortDir


    export interface SearchParams<TFilter extends SplineRecord = {}, TSortableField = string> {
        pager: QueryPager
        filter: TFilter
        alwaysOnFilter: TFilter
        sortBy: QuerySorter.FieldSorter<TSortableField>[]
        searchTerm: string
    }

    export const DEFAULT_SEARCH_PARAMS: SearchParams<any, any> = Object.freeze({
        pager: { ...DEFAULT_PAGER },
        filter: {},
        alwaysOnFilter: {},
        sortBy: [],
        searchTerm: '',
    })

    export type DataState<TData> = {
        data: TData | null
        loadingProcessing: ProcessingStore.EventProcessingState
    }

    export const DEFAULT_RENDER_DATA: DataState<any> = {
        data: null,
        loadingProcessing: ProcessingStore.getDefaultProcessingState(),
    }

    export const DEFAULT_SERVER_POLL_INTERVAL = 5000 // msec

    export function toMatSortable(fieldSorter: QuerySorter.FieldSorter, disableClear = true): MatSortable {
        return {
            id: fieldSorter.field,
            start: fieldSorter.dir.toLowerCase(),
            disableClear,
        } as MatSortable
    }

    export function matSortToFiledSorter<TFiled = string>(sort: Sort): FieldSorter<TFiled> {
        return {
            field: sort.active as any,
            dir: sort.direction === 'asc' ? SortDir.ASC : SortDir.DESC,
        }
    }

}
