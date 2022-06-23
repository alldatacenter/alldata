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

import { DtCellValueControlEvent } from './dynamic-table.models'


export type THeaderCellValue<T> = T | (() => T) | Observable<T> | (() => Observable<T>);

export interface DtHeaderCellSchema<T, TOptions extends SplineRecord = {}> {
    header?: THeaderCellValue<T>
    headerType?: string
    headerOptions?: THeaderCellValue<TOptions>
    headerClasses?: string[]
    isSortable?: boolean
    // isReorderable?: boolean
    // isResizable?: boolean
}

export interface IDTHeaderCellValueControl<T = string, TOptions extends SplineRecord = {}> {
    schema: DtHeaderCellSchema<T, TOptions>
    event$: EventEmitter<DtCellValueControlEvent>
}

export interface IDtHeaderCellFactory<T = string, TOptions extends SplineRecord = {}> extends IDynamicComponentFactory {
    readonly componentType: Type<IDTHeaderCellValueControl<T, TOptions>>
}

export function getHeaderCellSchema<T, TOptions extends SplineRecord = {}>(
    header: THeaderCellValue<T>,
    headerType?: string,
    headerOptions: THeaderCellValue<TOptions> = {} as TOptions): DtHeaderCellSchema<T, TOptions> {

    return {
        header,
        headerType,
        headerOptions,
    }
}
