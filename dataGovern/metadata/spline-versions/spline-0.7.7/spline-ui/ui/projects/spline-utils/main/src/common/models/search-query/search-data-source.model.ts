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

import { CollectionViewer, DataSource } from '@angular/cdk/collections'
import { isEqual } from 'lodash-es'
import { BehaviorSubject, EMPTY, interval, isObservable, Observable, of, Subject } from 'rxjs'
import { catchError, filter, first, map, share, skip, switchMap, takeUntil, tap, withLatestFrom } from 'rxjs/operators'

import { ProcessingStore } from '../../../store'
import { whenPageVisible } from '../../rxjs-operators'
import { SplineRecord, TypeHelpers } from '../heplers'
import { PageResponse, QuerySorter } from '../query'

import { SearchQuery } from './search-query.models'
import DataState = SearchQuery.DataState
import DEFAULT_RENDER_DATA = SearchQuery.DEFAULT_RENDER_DATA
import DEFAULT_SEARCH_PARAMS = SearchQuery.DEFAULT_SEARCH_PARAMS
import SearchParams = SearchQuery.SearchParams
import isFunction = TypeHelpers.isFunction


export type SearchDataSourceConfig<TFilter extends SplineRecord, TSortableFields> = {
    defaultSearchParams: Partial<SearchParams<TFilter, TSortableFields>>,
    pollingInterval: number
}

export type SearchDataSourceConfigInput<TFilter extends SplineRecord, TSortableFields> =
    | SearchDataSourceConfig<TFilter, TSortableFields>
    | (() => SearchDataSourceConfig<TFilter, TSortableFields>)
    | Observable<SearchDataSourceConfig<TFilter, TSortableFields>>
    | Observable<() => SearchDataSourceConfig<TFilter, TSortableFields>>

export abstract class SearchDataSource<TDataRecord = unknown,
    TData extends PageResponse<TDataRecord> = PageResponse<TDataRecord>,
    TFilter extends SplineRecord = {},
    TSortableFields = string> implements DataSource<TDataRecord> {

    readonly dataState$: Observable<DataState<TData>>
    readonly searchParams$: Observable<SearchParams<TFilter, TSortableFields>>

    readonly loadingProcessing$: Observable<ProcessingStore.EventProcessingState>
    readonly loadingProcessingEvents: ProcessingStore.ProcessingEvents<DataState<TData>>
    readonly serverDataUpdates$: Observable<TData>
    readonly disconnected$: Observable<void>

    private readonly _dataState$: BehaviorSubject<DataState<TData>>
    private readonly _searchParams$: BehaviorSubject<SearchParams<TFilter, TSortableFields>>
    private /*readonly*/ _defaultSearchParamsProvider: () => SearchParams<TFilter, TSortableFields>
    private readonly _disconnected$: Subject<void>

    protected abstract getDataObserver(searchParams: SearchParams<TFilter, TSortableFields>): Observable<TData>

    protected constructor(configInput: SearchDataSourceConfigInput<TFilter, TSortableFields>) {
        this._disconnected$ = new Subject<void>()
        this.disconnected$ = this._disconnected$

        this._searchParams$ = new BehaviorSubject(undefined)
        this.searchParams$ = this._searchParams$

        const configLike$ = isObservable(configInput) ? configInput : of(configInput)
        const configProvider$ = configLike$
            .pipe(
                takeUntil(this._disconnected$),
                first(),
                map(configFnOrObj => {
                    return isFunction(configFnOrObj) ? configFnOrObj : (() => configFnOrObj)
                }),
                share()
            )

        this.asyncInitDefaultSearchParams(configProvider$)

        this._dataState$ = new BehaviorSubject(DEFAULT_RENDER_DATA)
        this.dataState$ = this._dataState$

        this.loadingProcessing$ = this.dataState$.pipe(map(data => data.loadingProcessing))
        this.loadingProcessingEvents = ProcessingStore.createProcessingEvents(
            this.dataState$, (state) => state.loadingProcessing,
        )

        this.serverDataUpdates$ =
            configProvider$.pipe(
                switchMap(configProvider => this.createServerDataUpdatePoller(configProvider().pollingInterval))
            )

        this.subscribeToSearchParams()
    }

    reset(): void {
        this.updateSearchParams(this._defaultSearchParamsProvider())
    }

    search(searchTerm: string): void {
        const searchParamsWithResetPagination = this.withResetPagination({ searchTerm })
        this.updateSearchParams(searchParamsWithResetPagination)
    }

    sort(sortBy: QuerySorter.FieldSorter<TSortableFields>[]): void {
        this.updateSearchParams(this.withResetPagination({ sortBy }))
    }

    setFilter(filterValue: TFilter): void {
        const searchParams = this.withResetPagination({ filter: filterValue })
        this.updateSearchParams(searchParams)
    }

    goToPage(pageIndex: number): void {
        const currentPager = this._searchParams$.getValue().pager
        if (currentPager.offset !== pageIndex) {
            this.updateSearchParams({
                pager: {
                    ...currentPager,
                    offset: pageIndex * currentPager.limit,
                },
            })
        }
        else {
            console.warn('Nothing to do. The current offset equals to the target offset.')
        }
    }

    nextPage(): void {
        const currentPager = this._searchParams$.getValue().pager
        this.updateSearchParams({
            pager: {
                ...currentPager,
                offset: currentPager.offset + currentPager.limit,
            },
        })
    }

    prevPage(): void {
        const currentPager = this._searchParams$.getValue().pager
        if (currentPager.offset === 0) {
            console.error('You are already on the very first page, you cannot go back.')
            return
        }
        this.updateSearchParams({
            pager: {
                ...currentPager,
                offset: currentPager.offset - currentPager.limit,
            },
        })
    }

    update(searchParams: Partial<SearchParams<TFilter, TSortableFields>>): void {
        this.updateSearchParams(searchParams)
    }

    connect(collectionViewer: CollectionViewer): Observable<TDataRecord[]> {
        return this._dataState$
            .pipe(
                map(x => x.data?.items ?? []),
            )
    }

    disconnect(collectionViewer?: CollectionViewer): void {
        this._dataState$.complete()
        this._searchParams$.complete()

        this._disconnected$.next()
        this._disconnected$.complete()
    }

    private asyncInitDefaultSearchParams(configProvider$: Observable<() => SearchDataSourceConfig<TFilter, TSortableFields>>): void {
        configProvider$
            .subscribe(configProvider => {
                this._defaultSearchParamsProvider = () => {
                    const config = configProvider()
                    return {
                        ...DEFAULT_SEARCH_PARAMS,
                        ...config.defaultSearchParams,
                    }
                }
                this._searchParams$.next(this._defaultSearchParamsProvider())
            })
    }

    private subscribeToSearchParams(): void {
        this._searchParams$
            .pipe(
                skip(1), // skip default search params
                withLatestFrom(this._dataState$),
                tap(([, dataState]) => this.updateDataState({
                    loadingProcessing: ProcessingStore.eventProcessingStart(dataState.loadingProcessing),
                })),
                switchMap(([searchParams, dataState]) => this.getDataObserver(searchParams)
                    .pipe(
                        catchError((error) => {
                            this.updateDataState({
                                loadingProcessing: ProcessingStore.eventProcessingFinish(dataState.loadingProcessing, error),
                            })
                            return of(null)
                        })
                    )
                ),
                withLatestFrom(this._dataState$)
            )
            .subscribe(([result, dataState]) => {
                if (result !== null) {
                    this.updateDataState({
                        data: { ...result },
                        loadingProcessing: ProcessingStore.eventProcessingFinish(dataState.loadingProcessing),
                    })
                }
            })
    }

    private updateDataState(dataState: Partial<DataState<TData>>): void {
        this._dataState$.next({
            ...this._dataState$.getValue(),
            ...dataState,
        })
    }

    private withResetPagination(
        searchParams: Partial<SearchParams<TFilter, TSortableFields>>,
    ): Partial<SearchParams<TFilter, TSortableFields>> {
        return {
            ...searchParams,
            pager: {
                limit: this._searchParams$.getValue().pager.limit,
                offset: 0,
            },
        }
    }

    private updateSearchParams(searchParams: Partial<SearchParams<TFilter, TSortableFields>>): void {
        const newSearchParams = {
            ...this._searchParams$.getValue(),
            ...searchParams,
        } as SearchParams<TFilter, TSortableFields>

        this._searchParams$.next(newSearchParams)
    }

    private createServerDataUpdatePoller(pollingInterval: number): Observable<TData> {
        // Poll the server using the same search query as the main one, but with `asAtTime = now()`.
        // If the returned data differs from what the data source currently holds then an update notification
        // is sent to the observers.

        // The observable is multicast with active subscription counting.
        // The polling stops when `count == 0` and it resumes when `count > 0`

        return interval(pollingInterval)
            .pipe(
                whenPageVisible(),
                withLatestFrom(this._searchParams$),
                switchMap(([, lastSearchParams]) => {
                    const freshSearchParams = {
                        ...lastSearchParams,
                        filter: {
                            ...lastSearchParams.filter,
                            asAtTime: undefined
                        }
                    }
                    return this.getDataObserver(freshSearchParams)
                        .pipe(
                            first(),
                            catchError(() => EMPTY)
                        )
                }),
                share(),
                withLatestFrom(this._dataState$),
                filter(([serverData, lastDataState]) => !isEqual(serverData.items, lastDataState.data.items)),
                map(([serverData]) => serverData)
            )
    }
}

