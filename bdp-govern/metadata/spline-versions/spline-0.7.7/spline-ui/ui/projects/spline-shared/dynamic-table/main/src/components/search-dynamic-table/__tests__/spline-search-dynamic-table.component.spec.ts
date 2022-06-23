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

import { HttpClientTestingModule } from '@angular/common/http/testing'
import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { MatIconModule } from '@angular/material/icon'
import { MatPaginatorModule } from '@angular/material/paginator'
import { MatSortModule } from '@angular/material/sort'
import { MatTableModule } from '@angular/material/table'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'
import { NavigationEnd, Router } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { Observable, of } from 'rxjs'
import { filter, take } from 'rxjs/operators'
import { SplineSearchBoxModule, SplineSortHeaderModule } from 'spline-common'
import { DynamicTableDataMap, DynamicTableModule } from 'spline-common/dynamic-table'
import { SplineDynamicTableSharedModule, SplineSearchDynamicTable, SplineSearchDynamicTableComponent } from 'spline-shared/dynamic-table'
import { PageResponse, QuerySorter, SearchDataSource, SearchDataSourceConfigInput, SearchQuery } from 'spline-utils'
import { SplineTranslateTestingModule } from 'spline-utils/translate'
import SortDir = QuerySorter.SortDir;
import SearchParams = SearchQuery.SearchParams;
import DEFAULT_SEARCH_PARAMS = SearchQuery.DEFAULT_SEARCH_PARAMS;


describe('SplineSearchDynamicTableComponent', () => {

    let componentFixture: ComponentFixture<SplineSearchDynamicTableComponent<any>>
    let componentInstance: SplineSearchDynamicTableComponent<any>
    let router: Router

    beforeEach(async () =>
        TestBed.configureTestingModule({
            imports: [
                BrowserAnimationsModule,
                RouterTestingModule,
                HttpClientTestingModule,
                MatPaginatorModule,
                SplineSearchBoxModule,
                MatTableModule,
                MatSortModule,
                MatIconModule,
                SplineSortHeaderModule,
                DynamicTableModule,
                SplineDynamicTableSharedModule,
                SplineTranslateTestingModule
            ],
            schemas: [NO_ERRORS_SCHEMA, CUSTOM_ELEMENTS_SCHEMA],
        })
            .compileComponents()
    )

    beforeEach(() => {
        componentFixture = TestBed.createComponent<SplineSearchDynamicTableComponent>(SplineSearchDynamicTableComponent)
        componentInstance = componentFixture.componentInstance
        router = TestBed.inject<Router>(Router)
    })


    describe('Init Default State', () => {

        type FakeItem = {
            id: number
        }

        const dataMap: DynamicTableDataMap = [
            {
                id: 'id'
            }
        ]

        const fakeData: FakeItem[] = [
            {
                id: 1
            },
            {
                id: 2
            }
        ]

        class FakeDataSource extends SearchDataSource<FakeItem> {

            constructor(config: SearchDataSourceConfigInput<any, any>) {
                super(config)
            }


            protected getDataObserver(searchParams: SearchQuery.SearchParams): Observable<PageResponse<FakeItem>> {
                return of({
                    totalCount: fakeData.length,
                    items: [...fakeData]
                })
            }
        }

        let fakeDataSource: FakeDataSource

        const defaultSortBy: QuerySorter.FieldSorter = {
            field: 'id',
            dir: SortDir.DESC
        }

        beforeEach(() => {
            fakeDataSource = new FakeDataSource({
                defaultSearchParams: {
                    sortBy: [{ ...defaultSortBy }]
                },
                pollingInterval: -1
            })
            componentInstance.dataSource = fakeDataSource
            componentInstance.dataMap = dataMap
        })


        test('Init Sorting from DataSource', (done) => {

            componentFixture.detectChanges()

            componentInstance.state$
                .subscribe((state) => {
                    expect(state.sorting).toEqual(defaultSortBy)
                    done()
                })

        })

        test('Init SearchParams from Router QueryParams', (done) => {
            // define init router state
            const urlSorting: QuerySorter.FieldSorter = {
                field: 'id',
                dir: SortDir.ASC
            }

            const urlSearchParams: SearchParams = {
                ...DEFAULT_SEARCH_PARAMS,
                sortBy: [
                    urlSorting
                ]
            }

            const queryParams = SplineSearchDynamicTable.applySearchParams(
                {},
                componentInstance.defaultUrlStateQueryParamAlias,
                urlSearchParams
            )


            // fake router init state
            router.navigate([], {
                queryParams,
                replaceUrl: true,
            })

            router.events
                .pipe(
                    filter(event => event instanceof NavigationEnd),
                    take(1)
                )
                .subscribe(() => {
                    // init component after router state is init
                    componentFixture.detectChanges()

                    componentInstance.state$
                        .subscribe((state) => {
                            // expect comp state was initialized from router
                            expect(state.sorting).toEqual(urlSorting)

                            // expect DataSource search Params were sync with a Router state
                            expect(urlSearchParams.sortBy).toEqual([urlSorting])
                            done()
                        })
                })

        })

    })
})
