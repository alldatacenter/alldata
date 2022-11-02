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
import { RouterModule } from '@angular/router'
import { SplineIconModule } from 'spline-common'
import { SplineTranslateModule } from 'spline-utils/translate'

import { DT_CELL_FACTORY, DynamicTableModule } from '../../core'

import { DtCellIconComponent } from './dt-cell-icon.component'
import { DtCellIconFactory } from './dt-cell-icon.factory'


@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        SplineTranslateModule.forChild({}),
        DynamicTableModule,
        SplineIconModule
    ],
    declarations: [
        DtCellIconComponent
    ],
    exports: [
        DtCellIconComponent
    ],
    providers: [
        DtCellIconFactory,
        {
            provide: DT_CELL_FACTORY,
            useValue: DtCellIconFactory,
            multi: true
        }
    ]
})
export class DtCellIconModule {

}
