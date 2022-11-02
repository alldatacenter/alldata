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


import { Injectable } from '@angular/core'
import { select, Store } from '@ngrx/store'
import { Observable } from 'rxjs'
import { filter, switchMap, take } from 'rxjs/operators'
import { ExecutionEvent, OperationDetails, SplineDataSourceInfo } from 'spline-api'
import { ProcessingStore } from 'spline-utils'

import {
    DsOverviewDetailsStore,
    DsOverviewDetailsStoreActions,
    DsOverviewDetailsStoreSelectors,
    DsOverviewStoreSelectors
} from '../../store'


@Injectable()
export class DsOverviewDetailsStoreFacade {

    readonly state$: Observable<DsOverviewDetailsStore.State>
    readonly loading$: Observable<ProcessingStore.EventProcessingState>
    readonly isInitialized$: Observable<boolean>
    readonly dataSourceInfo$: Observable<SplineDataSourceInfo>
    readonly operationDetails$: Observable<OperationDetails[]>

    constructor(private readonly store: Store<any>) {

        this.state$ = this.store.pipe(select(DsOverviewDetailsStoreSelectors.rootState))
        this.isInitialized$ = this.store.pipe(select(DsOverviewStoreSelectors.isInitialized))
        this.loading$ = this.store.pipe(select(DsOverviewDetailsStoreSelectors.loading))

        this.operationDetails$ = this.store.pipe(select(DsOverviewDetailsStoreSelectors.operationDetails))

        this.dataSourceInfo$ = this.isInitialized$
            .pipe(
                filter(isInitialized => isInitialized),
                switchMap(() => this.store.pipe(select(DsOverviewStoreSelectors.dataSourceInfo))),
            )
    }

    init(executionEvent: ExecutionEvent): void {
        // first get the current value
        this.store.pipe(
            select(DsOverviewDetailsStoreSelectors.executionEvent),
            take(1),
            // take only new values
            filter(currentExecutionEvent => currentExecutionEvent?.executionEventId !== executionEvent?.executionEventId)
        )
            .subscribe(() =>
                // init new exec plan
                this.store.dispatch(
                    new DsOverviewDetailsStoreActions.Init({
                        executionEvent
                    })
                )
            )

    }

    unInit(): void {
        this.store.dispatch(
            new DsOverviewDetailsStoreActions.ResetState()
        )
    }
}
