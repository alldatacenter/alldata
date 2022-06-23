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

import { BehaviorSubject, Observable, Subject } from 'rxjs'
import { filter, map, takeUntil } from 'rxjs/operators'
import { SplineRecord } from 'spline-utils'

import {
    DynamicFilterControlsCollection,
    DynamicFilterControlsMap,
    IDynamicFilterControlModel
} from './dynamic-filter-control.models'


export type DynamicFilterValue<TFilter extends SplineRecord = SplineRecord> = {
    [K in keyof TFilter]: TFilter[K] | null;
};

export class DynamicFilterModel<TFilter extends SplineRecord = SplineRecord> {

    readonly value$: Observable<DynamicFilterValue<TFilter>>
    readonly valueChanged$: Observable<DynamicFilterValue<TFilter>>
    readonly isEmpty$: Observable<boolean>
    readonly controlsMap$: Observable<DynamicFilterControlsMap<TFilter>>
    readonly controls$: Observable<DynamicFilterControlsCollection<TFilter>>

    protected _state$ = new BehaviorSubject<{ value: DynamicFilterValue<TFilter>; emitEvent: boolean }>({
        value: {} as DynamicFilterValue<TFilter>,
        emitEvent: true,
    })

    protected destroyed$ = new Subject<void>()
    protected readonly _controlsMap$: BehaviorSubject<DynamicFilterControlsMap<TFilter>>

    constructor(filterControlsCollection: DynamicFilterControlsCollection<TFilter>) {
        // initialize models map
        const modelsMap = filterControlsCollection
            .reduce((result, model) => {
                if (result[model.id] === undefined) {
                    result[model.id] = model
                    this.initModel(model)
                }
                return result
            }, {} as DynamicFilterControlsMap<TFilter>)

        this._controlsMap$ = new BehaviorSubject<DynamicFilterControlsMap<TFilter>>(modelsMap)
        this.controlsMap$ = this._controlsMap$

        this.controls$ = this.controlsMap$
            .pipe(
                map((x) => Object.values(x))
            )

        this.valueChanged$ = this._state$
            .pipe(
                filter(({ emitEvent }) => emitEvent),
                map(({ value }) => value),
            )

        this.value$ = this._state$
            .pipe(
                map(({ value }) => value),
            )

        this.isEmpty$ = this.value$
            .pipe(
                map(
                    value => Object.keys(value)
                        .filter(key => value[key].value !== null)
                        .length === 0
                ),
            )
    }

    get controls(): DynamicFilterControlsCollection<TFilter> {
        return Object.values(this.controlsMap)
    }

    get controlsMap(): DynamicFilterControlsMap<TFilter> {
        return this._controlsMap$.getValue()
    }

    get value(): DynamicFilterValue<TFilter> {
        return this._state$.getValue().value
    }

    hasFilterControl(columnId: keyof TFilter): boolean {
        return this.controlsMap[columnId] !== undefined
    }

    getFilterControl(columnId: keyof TFilter): IDynamicFilterControlModel<TFilter[keyof TFilter], any, keyof TFilter> | undefined {
        return this.controlsMap[columnId]
    }

    patchValue(valueUpdate: TFilter, emitEvent = true): void {
        const filterValueInfo: DynamicFilterValue<TFilter> = Object.keys(this.controlsMap)
            .reduce(
                (result, columnId) => {
                    if (this.hasFilterControl(columnId as keyof TFilter)) {
                        // patch model value
                        const model = this.getFilterControl(columnId as keyof TFilter)
                        model.patchValue(valueUpdate[columnId], emitEvent)
                        // calculate filters value info
                        return {
                            ...result,
                            [columnId]: valueUpdate[columnId] ?? null
                        }
                    }

                    return result
                },
                {} as DynamicFilterValue<TFilter>
            )

        this.updateValue({ ...filterValueInfo }, emitEvent)
    }

    partialPatchValue(valueUpdate: Partial<TFilter>, emitEvent = true): void {
        const filterValueInfo: Partial<DynamicFilterValue<TFilter>> = Object.keys(valueUpdate)
            .reduce((result, columnId) => {
                if (this.hasFilterControl(columnId as keyof TFilter)) {
                    // patch model value
                    const model = this.getFilterControl(columnId as keyof TFilter)
                    model.patchValue(valueUpdate[columnId], emitEvent)
                    // calculate filters value info
                    return {
                        ...result,
                        [columnId]: valueUpdate[columnId] ?? null
                    }
                }
                return result
            }, {})

        if (!emitEvent) {
            this.updateValue(
                {
                    ...this.value,
                    ...filterValueInfo
                },
                false,
            )
        }
    }

    resetValue(emitEvent = true): void {
        const emptyValue = Object.keys(this.value)
            .reduce((result, colId) => {
                return {
                    ...result,
                    [colId]: null
                }
            }, {})

        this.patchValue(emptyValue as TFilter, emitEvent)
    }

    destroy(): void {
        this.destroyed$.next()
        this.destroyed$.complete()
    }

    protected updateValue(value: DynamicFilterValue<TFilter>, emitEvent = true): void {
        this._state$.next({ value: { ...value }, emitEvent })
    }

    protected initModel(model: IDynamicFilterControlModel<TFilter[keyof TFilter], any, keyof TFilter>): void {
        model.valueChanged$
            .pipe(
                takeUntil(this.destroyed$),
            )
            .subscribe((value: TFilter[keyof TFilter]) => {
                this.updateValue({
                    ...this.value,
                    [model.id]: value
                })
            })
    }

}
