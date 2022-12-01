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

import { EventEmitter, Type } from '@angular/core'
import { Observable } from 'rxjs'
import { IDynamicComponentFactory, SplineRecord } from 'spline-utils'

import { DtCellLayout } from './dt-row-layout.models'
import { DtCellValueControlEvent } from './dynamic-table.models'


export type TCellValueFn<T> = (rowData: any) => T;
export type TCellValue<T> = T | TCellValueFn<T> | TCellValueFn<Observable<T>>;

export interface DtCellValueSchema<T, TOptions extends SplineRecord = {}, TColumnId extends keyof any = string> {
    id: TColumnId
    type?: string
    options?: TCellValue<TOptions>
    value?: TCellValue<T>
    layout?: DtCellLayout
    isHidden?: boolean
    width?: string // should be a valid CSS width property value.
}

export interface IDTCellValueControl<T, TOptions extends SplineRecord = {}> {
    schema: DtCellValueSchema<T, TOptions>
    rowData: { [key: string]: any } | any[]
    event$: EventEmitter<DtCellValueControlEvent>
}

export interface IDtCellFactory<T = unknown, TOptions extends SplineRecord = {}> extends IDynamicComponentFactory {
    readonly componentType: Type<IDTCellValueControl<T, TOptions>>
}

export function getRowCellSchema<T, TOptions extends SplineRecord = {}>(
    id: string,
    value: TCellValue<T>,
    type?: string,
    options: TCellValue<TOptions> = {} as TOptions,
): DtCellValueSchema<T, TOptions> {

    return {
        id,
        type,
        options,
        value,
    }
}
