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


import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core'
import { PageEvent } from '@angular/material/paginator'
import { ActivatedRoute, NavigationEnd, Params, Router, RouterEvent } from '@angular/router'
import { isEqual } from 'lodash-es'
import { Observable, Subject } from 'rxjs'
import { distinctUntilChanged, filter, first, map, repeatWhen, skip, startWith, takeUntil } from 'rxjs/operators'
import {
    DtCellCustomEvent,
    DtHeaderCellCustomEvent,
    DynamicTableDataMap,
    DynamicTableOptions,
    getDefaultDtOptions
} from 'spline-common/dynamic-table'
import { BaseLocalStateComponent, QuerySorter, RouterNavigation, SearchDataSource, SplineRecord } from 'spline-utils'

import { SplineSearchDynamicTable } from './spline-search-dynamic-table.models'


@Component({
    selector: 'spline-search-dynamic-table',
    templateUrl: './spline-search-dynamic-table.component.html'
})
export class SplineSearchDynamicTableComponent<TRowData = undefined, TFilter extends SplineRecord = {}, TSortableFields = string>
    extends BaseLocalStateComponent<SplineSearchDynamicTable.State> implements OnInit, OnDestroy, OnChanges {

    readonly defaultUrlStateQueryParamAlias = 'searchTable'

    @Input() dataMap: DynamicTableDataMap
    @Input() dataSource: SearchDataSource<TRowData>

    @Input() options: Readonly<DynamicTableOptions> = getDefaultDtOptions()

    @Input() urlStateQueryParamAlias = this.defaultUrlStateQueryParamAlias
    @Input() isUrlStateDisabled = false
    @Input() showPaginator = true

    @Output() cellEvent$ = new EventEmitter<DtCellCustomEvent<TRowData>>()
    @Output() headerCellEvent$ = new EventEmitter<DtHeaderCellCustomEvent>()
    @Output() secondaryHeaderCellEvent$ = new EventEmitter<DtHeaderCellCustomEvent>()

    private readonly _resumeListeningOnServerUpdates$ = new Subject<void>()
    private _dataUpdateAvailable$: Observable<boolean>

    constructor(private readonly activatedRoute: ActivatedRoute,
                private readonly router: Router) {
        super()
        this.updateState(
            SplineSearchDynamicTable.getDefaultState()
        )
    }

    get currentQueryParams(): Params {
        return this.activatedRoute.snapshot.queryParams
    }

    get isDataUpdateAvailable$(): Observable<boolean> {
        return this._dataUpdateAvailable$
    }

    ngOnInit(): void {
        this.subscribeToRouter(this.router)
        this.subscribeToDataSource(this.dataSource)

        // start listening on the server data updates
        this.initDataUpdateAvailableObservable(this.dataSource)

        // init URL state Sync if it is allowed
        if (!this.isUrlStateDisabled) {
            this.initUrlStateSync(this.dataSource, this.urlStateQueryParamAlias)
        }

        // load data
        this.dataSource.update(this.searchParamsFromUrl() ?? {})
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { urlStateDisabled: isUrlStateDisabled } = changes

        if (isUrlStateDisabled
            && !isUrlStateDisabled.isFirstChange()
            && isUrlStateDisabled.previousValue !== isUrlStateDisabled.currentValue) {

            console.warn('[SplineSearchDynamicTable] :: isUrlStateDisabled changes are not considered once table was initialized.')
        }
    }

    onCellEvent($event: DtCellCustomEvent<TRowData>): void {
        this.cellEvent$.emit($event)
    }

    onHeaderCellEvent($event: DtHeaderCellCustomEvent): void {
        this.headerCellEvent$.emit($event)
    }

    onSecondaryHeaderCellEvent($event: DtHeaderCellCustomEvent): void {
        this.secondaryHeaderCellEvent$.emit($event)
    }

    onPaginationChanged(pageEvent: PageEvent): void {
        this.dataSource.goToPage(pageEvent.pageIndex)
    }

    onSearch(searchTerm: string): void {
        this.dataSource.search(searchTerm)
    }

    onRefreshDataClick(): void {
        this.dataSource.setFilter({ asAtTime: Date.now() })
        this._resumeListeningOnServerUpdates$.next()
    }

    ngOnDestroy(): void {
        this._resumeListeningOnServerUpdates$.complete()
        super.ngOnDestroy()
    }

    onSortingChanged(sorter: QuerySorter.FieldSorter): void {
        this.dataSource.sort([sorter])
    }

    private searchParamsFromUrl() {
        return !this.isUrlStateDisabled
            ? SplineSearchDynamicTable.extractSearchParamsFromUrl(
                this.currentQueryParams,
                this.urlStateQueryParamAlias,
            )
            : null
    }

    private subscribeToRouter(router: Router): void {
        // Refresh the table on re-navigating to the same route
        router.events.pipe(
            takeUntil(this.destroyed$),
            filter((event: RouterEvent) => event instanceof NavigationEnd)
        ).subscribe(() => {
            if (this.searchParamsFromUrl() === null) {
                this.dataSource.reset()
                this._resumeListeningOnServerUpdates$.next()
            }
        })
    }

    private subscribeToDataSource(dataSource: SearchDataSource<TRowData>): void {
        // totalCount
        dataSource.dataState$
            .pipe(
                takeUntil(this.destroyed$),
                map(dataState => dataState.data?.totalCount ?? 0),
                distinctUntilChanged()
            )
            .subscribe(
                totalCount => this.updateState({
                    totalCount
                })
            )

        // loadingProcessing
        dataSource.loadingProcessing$
            .pipe(
                takeUntil(this.destroyed$),
                skip(1),
                distinctUntilChanged((left, right) => isEqual(left, right))
            )
            .subscribe(
                loading => this.updateState({
                    loadingProcessing: loading
                })
            )

        // searchParams
        dataSource.searchParams$
            .pipe(
                takeUntil(this.destroyed$),
                skip(1),
                distinctUntilChanged((left, right) => isEqual(left, right))
            )
            .subscribe(searchParams => {
                const sorting = searchParams.sortBy.length > 0
                    ? searchParams.sortBy[0]
                    : null

                this.updateState({
                    sorting: sorting,
                    searchParams: { ...searchParams }
                })
            })
    }

    private initDataUpdateAvailableObservable(dataSource: SearchDataSource<TRowData>): void {
        this._dataUpdateAvailable$ = dataSource.serverDataUpdates$
            .pipe(
                takeUntil(this.destroyed$),
                map(() => true),
                first(),
                startWith(false),
                repeatWhen(() => this._resumeListeningOnServerUpdates$)
            )
    }

    private initUrlStateSync(dataSource: SearchDataSource<TRowData>, queryParamAlias: string): void {
        //
        // [ACTION] :: SEARCH PARAMS CHANGED
        //      => update URL
        //
        dataSource.searchParams$
            .pipe(
                takeUntil(this.destroyed$),
                distinctUntilChanged((a, b) => isEqual(a, b)),
                skip(1),
            )
            .subscribe((searchParams) => {
                const queryParams = SplineSearchDynamicTable.applySearchParams(
                    this.currentQueryParams,
                    queryParamAlias,
                    searchParams
                )
                this.updateRouterState(queryParams)
            })
    }

    private updateRouterState(queryParams, replaceUrl: boolean = true): void {
        RouterNavigation.updateCurrentRouterQueryParams(
            this.router,
            this.activatedRoute,
            queryParams,
            replaceUrl,
        )
    }


}
