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

import {
    ExecutionPlanLineageOverview,
    ExecutionPlanLineageOverviewDto,
    OperationAttributeLineage,
    OperationAttributeLineageDto,
    OperationDetails,
    OperationDetailsDto,
    toExecutionPlanLineageOverview,
    toOperationAttributeLineage,
    toOperationDetails,
} from '../models'

import { BaseFacade } from './base.facade'


@Injectable()
export class ExecutionPlanFacade extends BaseFacade {

    constructor(protected readonly http: HttpClient) {
        super(http)
    }

    fetchExecutionPlanDetails(executionPlanId: string): Observable<ExecutionPlanLineageOverview> {
        let params = new HttpParams()
        params = params.append('execId', executionPlanId)

        const url = this.toUrl('lineage-detailed')
        return this.http.get<ExecutionPlanLineageOverviewDto>(url, { params: params })
            .pipe(
                map(toExecutionPlanLineageOverview),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }

    fetchOperationDetails(operationId: string): Observable<OperationDetails> {
        const url = this.toUrl(`operations/${operationId}`)
        return this.http.get<OperationDetailsDto>(url)
            .pipe(
                map(toOperationDetails),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }

    fetchAttributeLineage(executionPlanId: string, attributeId: string): Observable<OperationAttributeLineage> {
        let params = new HttpParams()
        params = params.append('execId', executionPlanId)
        params = params.append('attributeId', attributeId)

        const url = this.toUrl('attribute-lineage-and-impact')

        return this.http.get<OperationAttributeLineageDto>(url, { params })
            .pipe(
                map(toOperationAttributeLineage),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }
}
