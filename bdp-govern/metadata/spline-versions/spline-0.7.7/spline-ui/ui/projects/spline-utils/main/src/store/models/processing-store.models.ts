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

import { Observable } from 'rxjs'
import { filter, switchMap, take } from 'rxjs/operators'


export namespace ProcessingStore {

    export interface ProcessingEvents<TState> {
        processingStart$: Observable<TState>
        processingEnd$: Observable<TState>
        success$: Observable<TState>
        error$: Observable<TState>
    }

    export interface EventProcessingState {
        processing: boolean
        processingError: null | any
    }

    export function getDefaultProcessingState(defaultProcessingState = false): EventProcessingState {
        return {
            processing: defaultProcessingState,
            processingError: null,
        }
    }

    export function createProcessingEvents<TState>(
        state$: Observable<TState>,
        selectEventProcessingStateFn: (state: TState) => EventProcessingState): ProcessingEvents<TState> {

        const processingStart$ = state$
            .pipe(
                filter(state => selectEventProcessingStateFn(state).processing),
            )

        const processingEnd$ = processingStart$
            .pipe(
                switchMap(() =>
                    state$
                        .pipe(
                            filter(state => !selectEventProcessingStateFn(state).processing),
                            take(1),
                        ),
                ),
            )

        const success$ = processingEnd$
            .pipe(
                filter(state => selectEventProcessingStateFn(state).processingError === null),
            )

        const error$ = processingEnd$
            .pipe(
                filter(state => selectEventProcessingStateFn(state).processingError !== null),
            )

        return {
            processingStart$,
            processingEnd$,
            success$,
            error$,
        }
    }

    export function eventProcessingStart(state: EventProcessingState): EventProcessingState {
        return {
            ...state,
            processing: true,
        }
    }

    export function eventProcessingFinish(state: EventProcessingState, error: null | any = null): EventProcessingState {
        return {
            ...state,
            processing: false,
            processingError: error,
        }
    }

    export function hasEventProcessingError(state: EventProcessingState): boolean {
        return state.processingError !== null
    }
}
