/*
 * Copyright 2020 ABSA Group Limited
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
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { MatButtonModule } from '@angular/material/button'
import { MatDividerModule } from '@angular/material/divider'
import { MatIconModule } from '@angular/material/icon'
import { MatListModule } from '@angular/material/list'
import { MatTooltipModule } from '@angular/material/tooltip'
import { SplineUtilsCommonModule } from 'spline-utils'
import { SplineTranslateModule } from 'spline-utils/translate'

import { SplineDividerModule } from '../divider'
import { SplineIconModule } from '../icon'
import { SplineLabelModule } from '../label'
import { SplineNoResultModule } from '../no-result'
import { SplineSearchBoxModule } from '../search-box'

import { SplineListBoxComponent } from './components'
import { SplineListBoxRecordsDirective } from './directives'


@NgModule({
    declarations: [
        SplineListBoxComponent,
        SplineListBoxRecordsDirective
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,

        MatIconModule,
        MatDividerModule,
        MatButtonModule,
        MatListModule,
        MatTooltipModule,

        SplineTranslateModule,
        SplineUtilsCommonModule,
        SplineLabelModule,
        SplineIconModule,
        SplineDividerModule,
        SplineSearchBoxModule,
        SplineNoResultModule,
    ],
    exports: [
        SplineListBoxComponent,
        SplineListBoxRecordsDirective
    ],
})
export class SplineListBoxModule {
}
