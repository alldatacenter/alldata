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

import { EventEmitter, InjectionToken } from '@angular/core'
import { BehaviorSubject, Observable, Subject } from 'rxjs'
import { filter, map } from 'rxjs/operators'
import { GenericEvent, IDynamicComponentFactory, SplineRecord } from 'spline-utils'


export interface IDynamicFilterControlModel<TValue = unknown,
    TOptions extends SplineRecord = SplineRecord, TId extends keyof any = any> {
    readonly id: TId
    readonly type: string
    readonly options: TOptions
    readonly value: TValue
    readonly value$: Observable<TValue>
    readonly valueChanged$: Observable<TValue>
    patchValue: (value: TValue, emitEvent: boolean) => void
}

export type DynamicFilterControlModelConfig<TValue, TOptions extends SplineRecord = SplineRecord> = {
    options?: TOptions
}

export type DynamicFilterControlSchema<TValue = unknown, TOptions extends SplineRecord = SplineRecord,
    TId extends keyof any = any> =
    & SplineRecord
    &
    {
        id: TId
        type: string
        options?: TOptions
    }

export type DynamicFilterSchema<TFilter extends SplineRecord = SplineRecord, TId extends keyof TFilter = keyof TFilter>
    = DynamicFilterControlSchema<TFilter[TId], any, TId>[]


export abstract class BaseDynamicFilterControlModel<TValue, TOptions extends SplineRecord = SplineRecord, TId extends keyof any = any>
implements IDynamicFilterControlModel<TValue, TOptions, TId> {

    readonly id: TId
    readonly valueChanged$: Observable<TValue>
    readonly value$: Observable<TValue>

    protected readonly destroyed$ = new Subject<void>()
    protected _options: TOptions = {} as TOptions
    protected _state$ = new BehaviorSubject<{ value: TValue; emitEvent: boolean }>({
        value: null,
        emitEvent: true,
    })
    abstract readonly type: string

    protected constructor(id: TId, config?: DynamicFilterControlModelConfig<TValue, TOptions>) {
        this.id = id

        if (config?.options !== undefined) {
            this._options = config.options
        }

        this.value$ = this._state$
            .pipe(
                map(({ value }) => value),
            )

        this.valueChanged$ = this._state$
            .pipe(
                filter(({ emitEvent }) => emitEvent),
                map(({ value }) => value),
            )
    }

    get value(): TValue {
        return this._state$.getValue().value
    }

    get options(): TOptions {
        return this._options
    }

    patchValue(value: TValue, emitEvent = true): void {
        this._state$.next({ value, emitEvent })
    }

    destroy(): void {
        this.destroyed$.next()
        this.destroyed$.complete()
    }
}

export type DynamicFilterControlsMap<TFilter extends SplineRecord> = {
    [TKey in keyof TFilter]: IDynamicFilterControlModel<TFilter[TKey], any, TKey>
}

export type DynamicFilterControlsCollection<TFilter extends SplineRecord, TKey extends keyof TFilter = keyof TFilter>
    = IDynamicFilterControlModel<TFilter[TKey], any, TKey>[]

export interface IDynamicFilterControlComponent<TValue = unknown, TOptions extends SplineRecord = SplineRecord,
    TId extends keyof any = any> {

    model: IDynamicFilterControlModel<TValue, TOptions, TId>
    event$: EventEmitter<GenericEvent>
}

export interface IDynamicFilterControlFactory<TValue = unknown, TOptions extends SplineRecord = SplineRecord>
    extends IDynamicComponentFactory<IDynamicFilterControlComponent<TValue, TOptions>> {

    createModelFromSchema<TFilterValue extends TValue = TValue,
        TFilterOptions extends TOptions = TOptions, TId extends keyof any = any>(
        schema: DynamicFilterControlSchema<TValue, TOptions, TId>, defaultValue?: TValue
    ): Observable<IDynamicFilterControlModel<TValue, TOptions, TId>>
}

export const DF_CONTROL_FACTORY = new InjectionToken<IDynamicFilterControlFactory<any>[]>('DF_CONTROL_FACTORY')
