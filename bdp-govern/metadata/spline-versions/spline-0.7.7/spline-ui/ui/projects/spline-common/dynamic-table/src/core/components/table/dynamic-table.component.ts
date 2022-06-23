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

import { DataSource } from '@angular/cdk/collections'
import { CdkTable } from '@angular/cdk/table'
import { AfterViewInit, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core'
import { MatSort, Sort } from '@angular/material/sort'
import { BaseLocalStateComponent, QuerySorter } from 'spline-utils'

import {
    DtCellClickedEvent,
    DtCellCustomEvent,
    DtCellValueControlEvent,
    DtHeaderCellCustomEvent,
    DynamicTableColumnSchema,
    DynamicTableDataMap,
    DynamicTableOptions,
    getDefaultDtOptions
} from '../../models'

import { DynamicTableStore } from './dynamic-table.store.models'


@Component({
    selector: 'dynamic-table',
    templateUrl: './dynamic-table.component.html',
})
export class DynamicTableComponent<TRowData> extends BaseLocalStateComponent<DynamicTableStore.State>
    implements OnChanges, AfterViewInit {

    @Input() dataSource: DataSource<TRowData> | TRowData[]
    @Input() dataMap: DynamicTableDataMap

    @Input() options: Readonly<DynamicTableOptions> = getDefaultDtOptions()
    @Input() sorting: Readonly<QuerySorter.FieldSorter>

    // events
    @Output() sortingChanged$ = new EventEmitter<QuerySorter.FieldSorter>()
    @Output() rowClicked$ = new EventEmitter<TRowData>()
    @Output() cellClicked$ = new EventEmitter<DtCellClickedEvent<TRowData>>()
    @Output() cellEvent$ = new EventEmitter<DtCellCustomEvent<TRowData>>()
    @Output() headerCellEvent$ = new EventEmitter<DtHeaderCellCustomEvent>()
    @Output() secondaryHeaderCellEvent$ = new EventEmitter<DtHeaderCellCustomEvent>()

    @ViewChild('tableRef', { static: true }) tableRef: CdkTable<TRowData>
    @ViewChild(MatSort, { static: false }) sortControl: MatSort

    readonly defaultDtOptions = getDefaultDtOptions()

    constructor() {
        super()

        this.updateState(
            DynamicTableStore.getDefaultState()
        )
    }

    ngOnChanges(changes: SimpleChanges): void {

        const { dataMap } = changes

        // init data map
        if (dataMap && dataMap.currentValue) {
            this.updateState(
                DynamicTableStore.reduceDataMap(this.state, dataMap.currentValue)
            )
        }
    }

    ngAfterViewInit(): void {
    }

    onRowClicked(row: TRowData): void {
        this.rowClicked$.emit(row)
    }

    onCellClicked(rowData: TRowData, columnSchema: DynamicTableColumnSchema<unknown, unknown, unknown, unknown, unknown, any>): void {
        this.cellClicked$.emit({ rowData, columnSchema })
    }

    onSortChanged(sort: Sort): void {
        this.sortingChanged$.emit(QuerySorter.fromMatSort(sort))
    }

    onCellEvent(rowData: TRowData, colId: string, event: DtCellValueControlEvent): void {
        // emit event
        this.cellEvent$.emit({
            rowData,
            colId,
            event
        })
    }

    onHeaderCellEvent(colId: string, event: DtCellValueControlEvent): void {
        this.headerCellEvent$.emit({
            colId,
            event
        })
    }

}
