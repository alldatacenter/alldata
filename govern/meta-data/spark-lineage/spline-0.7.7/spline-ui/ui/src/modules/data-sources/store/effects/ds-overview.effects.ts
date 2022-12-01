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
import { SplineDataSourceFacade } from 'spline-api'

import { DsOverviewStoreActions } from '../actions'
import fromActions = DsOverviewStoreActions


@Injectable()
export class DsOverviewEffects {
    //
    // [ACTION] :: INIT :: REQUEST
    //
    @Effect()
    initRequest$ = this.actions$
        .pipe(
            ofType<fromActions.InitRequest>(fromActions.ActionTypes.InitRequest),
            switchMap(({ payload }) =>
                this.splineDataSourceFacade.fetchOne(payload.dataSourceId)
                    .pipe(
                        catchError((error) => {
                            this.store.dispatch(
                                new fromActions.InitError({ dataSourceId: payload.dataSourceId, error })
                            )
                            return of(null)
                        })
                    )
            ),
            filter(entity => entity !== null),
            map(dataSourceInfo => {
                return new fromActions.InitSuccess({
                    dataSourceInfo
                })
            })
        )

    constructor(protected readonly actions$: Actions,
                protected readonly store: Store<any>,
                protected readonly splineDataSourceFacade: SplineDataSourceFacade) {

    }

}
