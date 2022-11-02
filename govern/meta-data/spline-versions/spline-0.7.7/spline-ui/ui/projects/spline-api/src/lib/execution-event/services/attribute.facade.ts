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

import { HttpClient, HttpParams } from '@angular/common/http'
import { Injectable } from '@angular/core'
import { Observable, throwError } from 'rxjs'
import { catchError, map } from 'rxjs/operators'

import { AttributeSearchRecord, AttributeSearchRecordDto, toAttributeSearchRecord } from '../models'

import { BaseFacade } from './base.facade'


@Injectable()
export class AttributeFacade extends BaseFacade {

    constructor(protected readonly http: HttpClient) {
        super(http)
    }

    search(searchTerm: string): Observable<AttributeSearchRecord[]> {
        let params = new HttpParams()
        params = params.append('search', searchTerm)

        const url = this.toUrl('attribute-search')

        return this.http.get<AttributeSearchRecordDto[]>(url, { params })
            .pipe(
                map(data => data.map(toAttributeSearchRecord)),
                catchError(error => {
                    console.error(error)
                    return throwError(error)
                })
            )
    }
}
