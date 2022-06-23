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
import { EventsDataSource } from 'spline-shared/events'
import { BaseComponent } from 'spline-utils'

import { EventsListDtSchema } from '../../dynamic-table'

import { EventsListPage } from './events-list.page.models'


@Component({
    selector: 'events-list-page',
    templateUrl: './events-list.page.component.html',
    styleUrls: ['./events-list.page.component.scss'],
    providers: [
        {
            provide: EventsDataSource,
            useFactory: (
                executionEventFacade: ExecutionEventFacade,
                splineConfigService: SplineConfigService,
            ) => {
                return new EventsDataSource(executionEventFacade, splineConfigService)
            },
            deps: [
                ExecutionEventFacade,
                SplineConfigService
            ],
        },
    ],
})
export class EventsListPageComponent extends BaseComponent implements OnDestroy, OnInit {

    readonly dataMap = EventsListDtSchema.getSchema()

    filterModel: DynamicFilterModel<EventsListPage.Filter>

    constructor(readonly dataSource: EventsDataSource,
                private readonly dynamicFilterFactory: DynamicFilterFactory) {
        super()
    }

    ngOnInit(): void {
        this.dynamicFilterFactory
            .schemaToModel<EventsListPage.Filter>(
                EventsListPage.getDynamicFilterSchema()
            )
            .pipe(
                takeUntil(this.destroyed$)
            )
            .subscribe(model => {
                this.filterModel = model
                DataSourceWithDynamicFilter.bindDynamicFilter<ExecutionEventsQuery.QueryFilter, EventsListPage.Filter>(
                    this.dataSource,
                    this.filterModel,
                    EventsListPage.getFiltersMapping()
                )
            })
    }

    ngOnDestroy(): void {
        super.ngOnDestroy()
        this.dataSource.disconnect()
    }
}
