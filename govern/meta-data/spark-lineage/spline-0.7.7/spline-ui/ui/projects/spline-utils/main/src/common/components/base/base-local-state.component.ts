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

import { Component } from '@angular/core'
import { BehaviorSubject } from 'rxjs'

import { BaseComponent } from './base.component'

@Component({
    selector: 'spline-utils-base-local-state',
    template: ''
})
export abstract class BaseLocalStateComponent<TState> extends BaseComponent {

    readonly state$ = new BehaviorSubject<TState | null>(null)

    protected constructor() {
        super()
    }

    get state(): TState {
        return this.state$.getValue()
    }

    protected updateState(state: Partial<TState>): TState {

        const newValue = {
            ...this.state,
            ...state,
        } as TState

        this.state$.next(newValue)

        return newValue
    }

}
