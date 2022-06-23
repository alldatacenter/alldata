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

import { CommonModule } from '@angular/common'
import { NgModule } from '@angular/core'
import { SplineDateRangeFilterModule } from 'spline-common'
import { DF_CONTROL_FACTORY, DynamicFilterControlManager, DynamicFilterModule } from 'spline-common/dynamic-filter'

import { DfControlDateRangeComponent } from './df-control-date-range.component'
import { DfControlDateRangeFactory } from './df-control-date-range.factory'


@NgModule({
    imports: [
        CommonModule,
        DynamicFilterModule,

        SplineDateRangeFilterModule
    ],
    declarations: [
        DfControlDateRangeComponent
    ],
    exports: [
        DfControlDateRangeComponent
    ],
    providers: [
        DfControlDateRangeFactory,
        {
            provide: DF_CONTROL_FACTORY,
            useValue: DfControlDateRangeFactory,
            multi: true
        }
    ]
})
export class DfControlDateRangeModule {
    constructor(private readonly dynamicFilterControlManager: DynamicFilterControlManager,
                private readonly dfControlDateRangeFactory: DfControlDateRangeFactory) {

        this.dynamicFilterControlManager.registerStaticFactory(
            this.dfControlDateRangeFactory
        )
    }
}
