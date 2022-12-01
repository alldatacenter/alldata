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

import { Component, OnInit } from '@angular/core'
import { Observable } from 'rxjs'
import { map, takeUntil } from 'rxjs/operators'
import { ExecutionEvent, ExecutionEventFacade, ExecutionEventsQuery, SplineDataSourceInfo } from 'spline-api'
import { DynamicFilterFactory, DynamicFilterModel } from 'spline-common/dynamic-filter'
import { DtCellCustomEvent } from 'spline-common/dynamic-table'
import { DataSourceWithDynamicFilter, SplineConfigService } from 'spline-shared'
import { BaseLocalStateComponent } from 'spline-utils'

import { DsStateHistoryDataSource } from '../../../data-sources'
import { DsStateHistoryDtSchema } from '../../../dynamic-table'
import { DsOverviewStoreFacade } from '../../../services'

import { DsOverviewHistoryPage } from './ds-overview-history.page.models'


@Component({
    selector: 'data-sources-overview-history-page',
    templateUrl: './ds-overview-history.page.component.html',
    styleUrls: ['./ds-overview-history.page.component.scss'],
    providers: [
        {
            provide: DsStateHistoryDataSource,
            useFactory: (
                executionEventFacade: ExecutionEventFacade,
                splineConfigService: SplineConfigService,
                store: DsOverviewStoreFacade) => {

                const dsUri$ = store.dataSourceInfo$.pipe(map((dataSourceInfo) => dataSourceInfo.uri))

                return new DsStateHistoryDataSource(executionEventFacade, splineConfigService, dsUri$)
            },
            deps: [
                ExecutionEventFacade,
                SplineConfigService,
                DsOverviewStoreFacade,
            ],
        },
    ],

})
export class DsOverviewHistoryPageComponent extends BaseLocalStateComponent<DsOverviewHistoryPage.State> implements OnInit {

    readonly dataMap = DsStateHistoryDtSchema.getSchema()
    readonly dataSourceInfo$: Observable<SplineDataSourceInfo>

    filterModel: DynamicFilterModel<DsOverviewHistoryPage.Filter>

    constructor(readonly dataSource: DsStateHistoryDataSource,
                private readonly dynamicFilterFactory: DynamicFilterFactory,
                private readonly store: DsOverviewStoreFacade) {
        super()

        this.updateState(
            DsOverviewHistoryPage.getDefaultState()
        )

        this.dataSourceInfo$ = this.store.dataSourceInfo$
    }

    ngOnInit(): void {
        this.dynamicFilterFactory
            .schemaToModel<DsOverviewHistoryPage.Filter>(
                DsOverviewHistoryPage.getDynamicFilterSchema()
            )
            .pipe(
                takeUntil(this.destroyed$)
            )
            .subscribe(model => {
                this.filterModel = model
                DataSourceWithDynamicFilter.bindDynamicFilter<ExecutionEventsQuery.QueryFilter, DsOverviewHistoryPage.Filter>(
                    this.dataSource,
                    this.filterModel,
                    DsOverviewHistoryPage.getFiltersMapping()
                )
            })
    }

    onCellEvent($event: DtCellCustomEvent<ExecutionEvent>): void {
        if ($event.event instanceof DsStateHistoryDtSchema.OpenDsStateDetailsEvent) {
            this.updateState(
                DsOverviewHistoryPage.reduceSelectDsState(
                    this.state, $event.rowData
                )
            )
        }
    }

    onSideDialogClosed(): void {
        this.updateState(
            DsOverviewHistoryPage.reduceSelectDsState(
                this.state, null
            )
        )
    }
}
