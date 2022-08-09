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
import { Actions, Effect, ofType } from '@ngrx/effects'
import { Store } from '@ngrx/store'
import { of } from 'rxjs'
import { catchError, filter, map, switchMap } from 'rxjs/operators'
import { ExecutionPlanFacade, executionPlanIdToWriteOperationId } from 'spline-api'

import { DsOverviewDetailsStoreActions } from '../actions'
import fromActions = DsOverviewDetailsStoreActions


@Injectable()
export class DsOverviewDetailsEffects {
    //
    // [ACTION] :: INIT :: REQUEST
    //
    @Effect()
    init$ = this.actions$
        .pipe(
            ofType<fromActions.Init>(fromActions.ActionTypes.Init),
            switchMap(({ payload }) =>
                this.executionPlanFacade.fetchOperationDetails(executionPlanIdToWriteOperationId(payload.executionEvent.executionPlanId))
                    .pipe(
                        catchError((error) => {
                            this.store.dispatch(
                                new fromActions.InitError({ error })
                            )
                            return of(null)
                        }),
                        map((oneOperationsDetails) => ({
                            operationsDetails: [oneOperationsDetails],
                            executionEvent: payload.executionEvent
                        }))
                    )
            ),
            filter(entity => entity !== null),
            map(result => {
                return new fromActions.InitSuccess({
                    operationsDetails: result.operationsDetails,
                    executionEvent: result.executionEvent,
                })
            })
        )


    constructor(protected readonly actions$: Actions,
                protected readonly store: Store<any>,
                private readonly executionPlanFacade: ExecutionPlanFacade) {

    }

}
