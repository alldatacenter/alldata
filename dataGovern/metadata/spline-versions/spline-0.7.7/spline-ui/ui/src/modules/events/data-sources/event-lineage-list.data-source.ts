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

import { Observable } from 'rxjs'
import { map } from 'rxjs/operators'
import { ExecutionEventFacade, ExecutionEventField, ExecutionEventLineageList } from 'spline-api'
import { PageResponse, QuerySorter, SearchDataSource, SearchQuery } from 'spline-utils'
import SortDir = QuerySorter.SortDir
import { SplineConfigService } from 'spline-shared'


export class EventLineageListDataSource extends SearchDataSource<ExecutionEventLineageList.LineageRecord> {

    private _executionEventId: string

    set executionEventId(value: string) {
        this._executionEventId = value
    }

    constructor(
        private readonly executionEventFacade: ExecutionEventFacade,
        private readonly splineConfigService: SplineConfigService,
    ) {
        super(() => ({
            defaultSearchParams: {
                filter: {
                    asAtTime: new Date().getTime()
                },
                sortBy: [
                    {
                        field: ExecutionEventField.timestamp,
                        dir: SortDir.DESC
                    }
                ]
            },
            pollingInterval: splineConfigService.config.serverPollingIntervalMs
        }))
    }

    protected getDataObserver(
        searchParams: SearchQuery.SearchParams
    ): Observable<PageResponse<ExecutionEventLineageList.LineageRecord>> {
        return this.executionEventFacade.fetchLineageOverview(this._executionEventId)
            .pipe(
                map(
                    lineageOverview => {
                        const list = ExecutionEventLineageList.toLineageList(lineageOverview.lineage)
                        return {
                            items: list,
                            totalCount: list.length
                        }
                    }
                )
            )
    }

}
