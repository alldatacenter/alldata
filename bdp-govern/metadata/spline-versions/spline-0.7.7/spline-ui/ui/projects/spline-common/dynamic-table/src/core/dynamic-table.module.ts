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

import { ScrollingModule } from '@angular/cdk/scrolling'
import { CommonModule } from '@angular/common'
import { NgModule } from '@angular/core'
import { MatIconModule } from '@angular/material/icon'
import { MatSortModule } from '@angular/material/sort'
import { MatTableModule } from '@angular/material/table'
import { RouterModule } from '@angular/router'
import { SplineLongTextModule, SplineSortHeaderModule, SplineVirtualScrollModule } from 'spline-common'
import { SplineUtilsCommonModule } from 'spline-utils'
import { SplineTranslateModule } from 'spline-utils/translate'

import { dynamicTableComponents, dynamicTableEntryComponents } from './components'
import * as fromServices from './services'


@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        MatTableModule,
        MatSortModule,
        MatIconModule,
        ScrollingModule,
        SplineUtilsCommonModule,
        SplineLongTextModule,
        SplineTranslateModule.forChild({}),
        SplineSortHeaderModule,
        SplineVirtualScrollModule,
    ],
    declarations: [
        ...dynamicTableComponents,
        ...dynamicTableEntryComponents,
    ],
    exports: [
        ...dynamicTableComponents,
        ...dynamicTableEntryComponents,
    ],
    providers: [
        ...fromServices.services,
    ],
    entryComponents: [
        ...dynamicTableEntryComponents,
    ]
})
export class DynamicTableModule {
}
