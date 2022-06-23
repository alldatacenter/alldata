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
import { SplineUtilsCommonModule } from 'spline-utils'

import { DT_CELL_FACTORY, DynamicTableModule } from '../../core'

import { DtCellElapsedTimeFactory } from './dt-cell-elapsed-time-factory.service'
import { DtCellElapsedTimeComponent } from './dt-cell-elapsed-time.component'


@NgModule({
    imports: [
        CommonModule,
        SplineUtilsCommonModule,
        DynamicTableModule,
    ],
    declarations: [
        DtCellElapsedTimeComponent
    ],
    exports: [
        DtCellElapsedTimeComponent
    ],
    providers: [
        DtCellElapsedTimeFactory,
        {
            provide: DT_CELL_FACTORY,
            useValue: DtCellElapsedTimeFactory,
            multi: true
        }
    ]
})
export class DtCellElapsedTimeModule {

}
