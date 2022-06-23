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

import { isEqual } from 'lodash-es'
import { Observable } from 'rxjs'
import { distinctUntilChanged, filter, map, skip, takeUntil, withLatestFrom } from 'rxjs/operators'
import { DynamicFilterModel } from 'spline-common/dynamic-filter'
import { SearchDataSource, SplineRecord } from 'spline-utils'


export namespace DataSourceWithDynamicFilter {

    export type DynamicFilterToQueryFilterMapFn<TQueryFilter extends SplineRecord, TDynamicFilter extends SplineRecord> =
        (dynamicFilter: TDynamicFilter, queryFilter?: TQueryFilter) => TQueryFilter

    export type QueryFilterToDynamicFilterMapFn<TQueryFilter extends SplineRecord, TDynamicFilter extends SplineRecord> =
        (queryFilter: TQueryFilter, dynamicFilterDefaultValue?: TDynamicFilter) => TDynamicFilter


    export type FiltersMapping<TQueryFilter extends SplineRecord, TDynamicFilter extends SplineRecord> = {
        dynamicFilterToQueryFilterMapFn: DynamicFilterToQueryFilterMapFn<TQueryFilter, TDynamicFilter>
        queryFilterToDynamicFilterMapFn: QueryFilterToDynamicFilterMapFn<TQueryFilter, TDynamicFilter>
    }

    // Synchronize DynamicFilter and DataSource filter values.
    export function bindDynamicFilter<TQueryFilter extends SplineRecord, TDynamicFilter extends SplineRecord>(
        dataSource: SearchDataSource<any, any, TQueryFilter>,
        filterModel: DynamicFilterModel<TDynamicFilter>,
        filtersMapping: FiltersMapping<TQueryFilter, TDynamicFilter>,
        bindTill$: Observable<void> = dataSource.disconnected$): void {

        //
        // [ACTION] :: DynamicFilter value changed
        //      => update DataSource filter & trigger data fetching
        //
        filterModel.valueChanged$
            .pipe(
                takeUntil(bindTill$),
                distinctUntilChanged((left, right) => isEqual(left, right)),
                withLatestFrom(dataSource.searchParams$),
                map(([filterValue, searchParams]) => {
                    const currentQueryFilter = searchParams.filter
                    return [
                        {
                            ...currentQueryFilter,
                            ...filtersMapping.dynamicFilterToQueryFilterMapFn(filterValue, currentQueryFilter)
                        },
                        currentQueryFilter]
                }),
                filter(([queryFilter, currentQueryFilter]) => !isEqual(queryFilter, currentQueryFilter))
            )
            .subscribe(([queryFilter]) => {
                dataSource.setFilter(queryFilter)
            })

        //
        // [ACTION] :: SearchParams changed
        //      => update current DynamicFilter filter value
        //
        dataSource.searchParams$
            .pipe(
                skip(1),
                takeUntil(bindTill$),
                map((searchParams) => filtersMapping.queryFilterToDynamicFilterMapFn(searchParams.filter)),
                filter(filterValue => {
                    const currentValue = filterModel.value
                    return !isEqual(filterValue, currentValue)
                }),
            )
            .subscribe(filterValue => {
                filterModel.patchValue(filterValue, false)
            })
    }

}
