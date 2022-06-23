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

import { SplineRecord } from 'spline-utils'

import { DtCellValueSchema } from './cell-value-control.models'
import { DtHeaderCellSchema } from './header-cell-value-control.models'
import { DtSecondaryHeaderCellSchema } from './secondary-header-cell-value-control.models'


export type DynamicTableDataMap<TColumnId extends keyof any = string> =
    DynamicTableColumnSchema<unknown, unknown, unknown, unknown, unknown, TColumnId>[]

export type DynamicTableColumnSchema<T = unknown, THeader = unknown,
    TSecondaryHeader = unknown, TOptions extends SplineRecord = {},
    TTitleOptions extends SplineRecord = {}, TColumnId extends keyof any = string> =
    & DtCellValueSchema<T, TOptions, TColumnId>
    & DtHeaderCellSchema<THeader, TTitleOptions>
    & DtSecondaryHeaderCellSchema<TSecondaryHeader, TTitleOptions>

export interface DtCellValueControlEvent<TData extends SplineRecord = SplineRecord> {
    readonly type: string
    readonly data?: TData
}

export class CommonDtCellValueControlEvent<TData extends SplineRecord = SplineRecord> implements DtCellValueControlEvent<TData> {
    readonly type: string
    constructor(readonly data: TData = {} as TData) {
    }
}

export interface DynamicTableOptions {
    isSticky?: boolean // stick to container
}

export function isDtColVisible(colSchema: DynamicTableColumnSchema<any>): boolean {
    return !colSchema?.isHidden
}

export type DtCellClickedEvent<T> = {
    rowData: T
    columnSchema: DynamicTableColumnSchema
}

export type DtHeaderCellCustomEvent = {
    colId: string
    event: DtCellValueControlEvent
}

export type DtCellCustomEvent<T extends SplineRecord = unknown> = {
    rowData: T
    colId: string
    event: DtCellValueControlEvent
}

export function getDefaultDtOptions(): DynamicTableOptions {
    return {
        isSticky: false,
    }
}

export function getDefaultStickyDtOptions(): DynamicTableOptions {
    return {
        ...getDefaultDtOptions(),
        isSticky: true
    }
}
