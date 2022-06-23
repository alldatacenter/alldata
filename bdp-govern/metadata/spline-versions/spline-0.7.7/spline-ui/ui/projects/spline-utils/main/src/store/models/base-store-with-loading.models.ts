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

import { BaseStore } from './base-store.models'
import { ProcessingStore } from './processing-store.models'


export type StateWithLoading = {
    loading: ProcessingStore.EventProcessingState
}

export abstract class BaseStoreWithLoading<TState extends StateWithLoading> extends BaseStore<TState> {

    readonly loadingEvents: ProcessingStore.ProcessingEvents<TState>
    readonly loading$: Observable<ProcessingStore.EventProcessingState>

    protected constructor(defaultState: TState = null) {

        super(defaultState)

        this.loadingEvents = ProcessingStore.createProcessingEvents(
            this.state$, (state) => state.loading,
        )

        this.loading$ = this.state$.pipe(map(data => data.loading))
    }

}
