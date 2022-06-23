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

import { Component, EventEmitter, Output } from '@angular/core'
import { BehaviorSubject, isObservable, Observable, Subject } from 'rxjs'
import { takeUntil } from 'rxjs/operators'
import { BaseComponent, SplineRecord } from 'spline-utils'

import { DtCellValueControlEvent } from '../../models'


@Component({
    selector: 'dt-base-cell-type',
    template: ''
})
export abstract class BaseCellTypeComponent<T, TOptions extends SplineRecord> extends BaseComponent {

    @Output() event$ = new EventEmitter<DtCellValueControlEvent>();

    value$ = new BehaviorSubject<T | null>(null);
    options$ = new BehaviorSubject<TOptions | null>({} as TOptions)

    get options(): TOptions {
        return this.options$.getValue()
    }

    get value(): T | null {
        return this.value$.getValue()
    }

    protected initValueFromSource<TValue>(value$: Subject<TValue | null>, valueSource: TValue | Observable<TValue>) {
        if (isObservable(valueSource)) {
            valueSource
                .pipe(
                    takeUntil(this.destroyed$),
                )
                .subscribe(
                    val => value$.next(val),
                )
        }
        // inline value
        else {
            value$.next(valueSource)
        }
    }
}


