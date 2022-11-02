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

import { Component, OnDestroy, OnInit } from '@angular/core'
import { takeUntil } from 'rxjs/operators'
import { ExecutionEventFacade, ExecutionEventsQuery } from 'spline-api'
import { DynamicFilterFactory, DynamicFilterModel } from 'spline-common/dynamic-filter'
import { DataSourceWithDynamicFilter, SplineConfigService } from 'spline-shared'
import { BaseComponent } from 'spline-utils'

import { SplineDataSourcesDataSource } from '../../data-sources'
import { DataSourcesListDtSchema } from '../../dynamic-table'

import { DataSourcesListPage } from './data-sources-list.page.models'


@Component({
    selector: 'data-sources-list-page',
    templateUrl: './data-sources-list.page.component.html',
    styleUrls: ['./data-sources-list.page.component.scss'],
    providers: [
        {
            provide: SplineDataSourcesDataSource,
            useFactory: (
                executionEventFacade: ExecutionEventFacade,
                splineConfigService: SplineConfigService,
            ) => {
                return new SplineDataSourcesDataSource(executionEventFacade, splineConfigService)
            },
            deps: [
                ExecutionEventFacade,
                SplineConfigService,
            ],
        },
    ],
})
export class DataSourcesListPageComponent extends BaseComponent implements OnDestroy, OnInit {

    readonly dataMap = DataSourcesListDtSchema.getSchema()

    filterModel: DynamicFilterModel<DataSourcesListPage.Filter>

    constructor(readonly dataSource: SplineDataSourcesDataSource,
                private readonly dynamicFilterFactory: DynamicFilterFactory) {
        super()
    }

    ngOnInit(): void {
        this.dynamicFilterFactory
            .schemaToModel<DataSourcesListPage.Filter>(
                DataSourcesListPage.getDynamicFilterSchema()
            )
            .pipe(
                takeUntil(this.destroyed$)
            )
            .subscribe(model => {
                this.filterModel = model
                DataSourceWithDynamicFilter.bindDynamicFilter<ExecutionEventsQuery.QueryFilter, DataSourcesListPage.Filter>(
                    this.dataSource,
                    this.filterModel,
                    DataSourcesListPage.getFiltersMapping()
                )
            })
    }

    ngOnDestroy(): void {
        super.ngOnDestroy()
        this.dataSource.disconnect()
    }

}
