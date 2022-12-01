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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core'
import { BehaviorSubject, isObservable, Observable, Subject } from 'rxjs'
import { takeUntil } from 'rxjs/operators'
import { BaseComponent } from 'spline-utils'

import { ISplineDataWidget, SdWidgetSchema, SplineDataWidgetEvent } from '../../models'


@Component({
    selector: 'sd-widget-base',
    template: ''
})
export abstract class SdWidgetBaseComponent<TData extends Record<string, any>, TOptions extends Record<string, any> = Record<string, any>>
    extends BaseComponent implements ISplineDataWidget<TData, TOptions>, OnInit {

    @Input() schema: SdWidgetSchema<TData, TOptions>
    @Input() isSelected: boolean

    @Output() event$ = new EventEmitter<SplineDataWidgetEvent>()

    readonly data$ = new BehaviorSubject<TData | null>(null)
    readonly options$ = new BehaviorSubject<TOptions>({} as TOptions)

    get options(): TOptions {
        return this.options$.getValue()
    }

    get data(): TData | null {
        return this.data$.getValue()
    }

    ngOnInit(): void {
        this.initDataFromSchema(this.schema)
        this.initOptionsFromSchema(this.schema)
    }

    protected initDataFromSchema(schema: SdWidgetSchema<TData, TOptions>): void {
        const schemaData = schema.data
        if (schemaData !== undefined) {
            const value = typeof schemaData === 'function'
                ? (schemaData as Function)()
                : schemaData
            this.initValueFromSource(this.data$, value)
        }
    }

    protected initOptionsFromSchema(schema: SdWidgetSchema<TData, TOptions>): void {
        const schemaOptions = schema.options
        if (schemaOptions !== undefined) {
            const value = typeof schemaOptions === 'function'
                ? (schemaOptions as Function)()
                : schemaOptions
            this.initValueFromSource(this.options$, value)
        }
    }

    protected initValueFromSource<TValue>(value$: Subject<TValue | null>, valueSource: TValue | Observable<TValue>): void {
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
