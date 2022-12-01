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

import { BehaviorSubject, Observable, of, Subject } from 'rxjs'
import { catchError, map, switchMap, take, takeUntil, tap } from 'rxjs/operators'

import { ProcessingStore } from '../../../store'
import { SplineRecord } from '../heplers'

import { SearchQuery } from './search-query.models'
import DataState = SearchQuery.DataState
import DEFAULT_RENDER_DATA = SearchQuery.DEFAULT_RENDER_DATA


export abstract class SimpleDataSource<TData, TFilter extends SplineRecord = {}> {

    readonly dataState$: Observable<DataState<TData>>
    readonly data$: Observable<TData>

    readonly loadingProcessing$: Observable<ProcessingStore.EventProcessingState>
    readonly loadingProcessingEvents: ProcessingStore.ProcessingEvents<DataState<TData>>

    readonly onFilterChanged$ = new Subject<{ filter: TFilter; apply: boolean; force: boolean }>()

    protected readonly _dataState$ = new BehaviorSubject<DataState<TData>>(DEFAULT_RENDER_DATA)
    protected readonly _filter$ = new BehaviorSubject<TFilter>({} as TFilter)

    protected readonly disconnected$ = new Subject<void>()

    protected constructor() {

        this.dataState$ = this._dataState$
        this.data$ = this.dataState$.pipe(map(data => data.data))

        this.loadingProcessing$ = this.dataState$.pipe(map(data => data.loadingProcessing))
        this.loadingProcessingEvents = ProcessingStore.createProcessingEvents(
            this.dataState$, (state) => state.loadingProcessing,
        )

        this.init()
    }

    get data(): TData {
        return this._dataState$.getValue().data
    }

    get filter(): TFilter {
        return this._filter$.getValue()
    }

    get dataState(): DataState<TData> {
        return this._dataState$.getValue()
    }

    setFilter(filterValue: TFilter, apply: boolean = true, force: boolean = false): void {
        this.onFilterChanged$.next({ filter: filterValue, apply, force })
    }

    connect(): Observable<TData> {
        return this._dataState$
            .pipe(
                map(x => x.data),
            )
    }

    disconnect(): void {
        this._dataState$.complete()
        this._filter$.complete()

        this.disconnected$.next()
        this.disconnected$.complete()
    }

    protected init(): void {
        // FILTER CHANGED EVENT
        this.onFilterChanged$
            .pipe(
                takeUntil(this.disconnected$),
                tap((payload) => this._filter$.next(payload.filter)),
                switchMap((payload) => this.fetchData(payload.filter)),
            )
            .subscribe()
    }

    private fetchData(filterValue: TFilter): Observable<TData> {

        this.updateDataState({
            loadingProcessing: ProcessingStore.eventProcessingStart(this.dataState.loadingProcessing),
        })

        return this.getDataObserver(filterValue)
            .pipe(
                catchError((error) => {
                    this.updateDataState({
                        loadingProcessing: ProcessingStore.eventProcessingFinish(this.dataState.loadingProcessing, error),
                    })
                    return of(null)
                }),
                // update data state
                tap((result) => {
                    if (result !== null) {
                        this.updateDataState({
                            data: result,
                            loadingProcessing: ProcessingStore.eventProcessingFinish(this.dataState.loadingProcessing),
                        })
                    }
                }),
                take(1),
            )
    }

    protected updateDataState(dataState: Partial<DataState<TData>>): void {
        this._dataState$.next({
            ...this._dataState$.getValue(),
            ...dataState,
        })
    }


    protected abstract getDataObserver(filterValue: TFilter): Observable<TData>;
}

