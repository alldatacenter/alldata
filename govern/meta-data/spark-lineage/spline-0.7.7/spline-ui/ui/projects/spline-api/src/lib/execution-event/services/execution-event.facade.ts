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

import { HttpClient, HttpParams } from '@angular/common/http'
import { Injectable } from '@angular/core'
import { Observable, throwError } from 'rxjs'
import { catchError, map } from 'rxjs/operators'

import { ExecutionEventsPageResponse, ExecutionEventsPageResponseDto, ExecutionEventsQuery, toExecutionEventsPageResponse } from '../models'
import {
    ExecutionEventLineageOverview,
    ExecutionEventLineageOverviewDto,
    toExecutionEventLineageOverview,
} from '../models/entities/execution-event-overview'

import { BaseFacade } from './base.facade'


@Injectable()
export class ExecutionEventFacade extends BaseFacade {

    constructor(protected readonly http: HttpClient) {
        super(http)
    }

    fetchLineageOverview(executionEventId: string, maxDepth: number = 3): Observable<ExecutionEventLineageOverview> {
        let params = new HttpParams()
        params = params.append('eventId', executionEventId)
        params = params.append('maxDepth', maxDepth.toString())

        const url = this.toUrl('lineage-overview')
        return this.http.get<ExecutionEventLineageOverviewDto>(url, { params: params })
            .pipe(
                map(toExecutionEventLineageOverview),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }

    fetchList(queryParams: ExecutionEventsQuery.QueryParams): Observable<ExecutionEventsPageResponse> {
        const params = ExecutionEventsQuery.queryParamsToHttpParams(queryParams)
        const url = this.toUrl('execution-events')
        return this.http.get<ExecutionEventsPageResponseDto>(url, { params: params })
            .pipe(
                map(toExecutionEventsPageResponse),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }

    fetchListAggregatedByDataSource(queryParams: ExecutionEventsQuery.QueryParams): Observable<ExecutionEventsPageResponse> {
        const params = ExecutionEventsQuery.queryParamsToHttpParams(queryParams)
        const url = this.toUrl('data-sources')
        return this.http.get<ExecutionEventsPageResponseDto>(url, { params: params })
            .pipe(
                map(toExecutionEventsPageResponse),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }
}
