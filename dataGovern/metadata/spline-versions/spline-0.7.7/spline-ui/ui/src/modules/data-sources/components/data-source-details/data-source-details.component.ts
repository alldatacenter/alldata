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

import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core'
import { ExecutionEvent, SplineDataSourceInfo } from 'spline-api'

import { DsOverviewDetailsStoreFacade } from '../../services'


@Component({
    selector: 'data-source-details',
    templateUrl: './data-source-details.component.html',
    styleUrls: ['./data-source-details.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataSourceDetailsComponent implements OnChanges {

    @Input() executionEvent: ExecutionEvent
    @Input() dataSourceInfo: SplineDataSourceInfo

    constructor(readonly store: DsOverviewDetailsStoreFacade) {
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { executionEvent } = changes
        if (executionEvent && executionEvent.currentValue) {
            const currentExecutionEvent = executionEvent.currentValue as ExecutionEvent
            this.store.init(currentExecutionEvent)
        }
    }
}
