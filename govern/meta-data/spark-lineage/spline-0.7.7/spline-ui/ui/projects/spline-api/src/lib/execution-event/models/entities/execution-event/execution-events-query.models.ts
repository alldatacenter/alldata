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

import { HttpParams } from '@angular/common/http'
import { DEFAULT_PAGE_LIMIT, PageQueryParams, QueryPager, QuerySorter, SearchQuery } from 'spline-utils'

import { DataSourceWriteMode } from '../data-source'

import { ExecutionEventField } from './execution-event.models'


export namespace ExecutionEventsQuery {

    export type QueryFilter = {
        executedAtFrom?: Date
        executedAtTo?: Date
        searchTerm?: string
        dataSourceUri?: string
        asAtTime?: number
        applicationId?: string
        writeMode?: DataSourceWriteMode[]
    }

    export type QueryParams = PageQueryParams<QueryFilter, ExecutionEventField>

    export type QueryParamsDto = {
        timestampStart?: number
        timestampEnd?: number
        sortOrder?: string
        sortField?: string
        searchTerm?: string
        pageSize?: number
        pageNum?: number
        dataSourceUri?: string
        asAtTime?: number
        applicationId?: string
        append?: boolean
    }

    export function toQueryParamsDto(queryParams: QueryParams): QueryParamsDto {
        return {
            ...(queryParams?.filter ? toQueryFilterDto(queryParams.filter) : {}),
            ...(queryParams?.pager ? toQueryPagerDto(queryParams.pager) : {}),
            ...(queryParams?.sortBy?.length ? toSortingDto(queryParams.sortBy) : {}),
        }
    }

    export function queryParamsDtoToHttpParams(queryParamsDto: QueryParamsDto): HttpParams {
        let httpParams = new HttpParams()
        Object.keys(queryParamsDto)
            .filter(key => queryParamsDto[key] !== undefined)
            .forEach(key => {
                httpParams = httpParams.append(key, queryParamsDto[key])
            })
        return httpParams
    }

    export function queryParamsToHttpParams(queryParams: QueryParams): HttpParams {
        const queryParamsDto = toQueryParamsDto(queryParams)
        return queryParamsDtoToHttpParams(queryParamsDto)
    }

    export function toQueryParams(
        searchParams: SearchQuery.SearchParams<QueryFilter, ExecutionEventField>,
    ): QueryParams {
        const queryFilter = {
            ...searchParams.filter,
            ...searchParams.alwaysOnFilter,
            searchTerm: searchParams.searchTerm,
        }
        return {
            filter: queryFilter,
            pager: searchParams.pager,
            sortBy: searchParams.sortBy,
        }
    }


    function toQueryFilterDto(queryFilter: QueryFilter): Partial<QueryParamsDto> {
        return {
            timestampStart: queryFilter?.executedAtFrom ? queryFilter?.executedAtFrom.getTime() : undefined,
            timestampEnd: queryFilter?.executedAtTo ? queryFilter?.executedAtTo.getTime() : undefined,
            searchTerm: queryFilter?.searchTerm?.length ? queryFilter?.searchTerm : undefined,
            dataSourceUri: queryFilter?.dataSourceUri,
            asAtTime: queryFilter?.asAtTime,
            applicationId: queryFilter?.applicationId,
            append: queryFilter?.writeMode
                ? (
                    queryFilter.writeMode?.length === 1
                        ? queryFilter.writeMode[0] === DataSourceWriteMode.Append
                        : undefined // there are just 2 options for now, if we have specified 2 options it means we want to see all items
                )
                : undefined
        }
    }

    function toQueryPagerDto(queryPager: Partial<QueryPager>): Partial<QueryParamsDto> {
        const limit = queryPager?.limit ?? DEFAULT_PAGE_LIMIT
        const offset = queryPager?.offset ?? 0
        return {
            pageSize: limit,
            pageNum: offset / limit + 1,
        }
    }

    function toSortingDto(sortBy: QuerySorter.FieldSorter<ExecutionEventField>[]): Partial<QueryParamsDto> {
        return {
            sortField: sortBy[0].field,
            sortOrder: sortBy[0].dir.toLowerCase(),
        }
    }
}
