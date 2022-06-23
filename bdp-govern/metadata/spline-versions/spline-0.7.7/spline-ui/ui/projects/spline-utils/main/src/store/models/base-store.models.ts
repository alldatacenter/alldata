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

import { BehaviorSubject, Observable, Subject } from 'rxjs'


export abstract class BaseStore<TState> {

    readonly state$: Observable<TState>

    protected readonly _defaultState: TState
    protected readonly _state$: BehaviorSubject<TState>
    protected readonly disconnected$ = new Subject<void>()

    protected constructor(defaultState: TState = null) {
        this._defaultState = defaultState
        this._state$ = new BehaviorSubject<TState>(this._defaultState)
        this.state$ = this._state$
    }

    get state(): TState {
        return this._state$.getValue()
    }

    resetState(): void {
        this.updateState(this._defaultState)
    }

    disconnect(): void {
        this._state$.complete()
        this.disconnected$.next()
        this.disconnected$.complete()
    }

    protected updateState(state: Partial<TState>): TState {

        const newValue = {
            ...this.state,
            ...state,
        } as TState

        this._state$.next(newValue)

        return newValue
    }

}
