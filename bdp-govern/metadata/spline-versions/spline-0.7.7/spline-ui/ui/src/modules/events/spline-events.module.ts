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
import { HttpClientModule } from '@angular/common/http'
import { NgModule } from '@angular/core'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { MatButtonModule } from '@angular/material/button'
import { MatCardModule } from '@angular/material/card'
import { MatDividerModule } from '@angular/material/divider'
import { MatIconModule } from '@angular/material/icon'
import { MatInputModule } from '@angular/material/input'
import { MatMenuModule } from '@angular/material/menu'
import { MatPaginatorModule } from '@angular/material/paginator'
import { MatSlideToggleModule } from '@angular/material/slide-toggle'
import { MatSortModule } from '@angular/material/sort'
import { MatTableModule } from '@angular/material/table'
import { MatTabsModule } from '@angular/material/tabs'
import { MatTooltipModule } from '@angular/material/tooltip'
import { MatTreeModule } from '@angular/material/tree'
import { RouterModule } from '@angular/router'
import { SplineApiModule } from 'spline-api'
import { SplineCommonModule, SplineListBoxModule } from 'spline-common'
import { SplineDataViewModule } from 'spline-common/data-view'
import { DynamicFilterModule } from 'spline-common/dynamic-filter'
import { DfControlDateRangeModule, DfControlSelectModule } from 'spline-common/dynamic-filter/filter-controls'
import { DynamicTableCommonCellsModule, DynamicTableModule } from 'spline-common/dynamic-table'
import { SplineGraphModule } from 'spline-common/graph'
import { SplineLayoutModule } from 'spline-common/layout'
import { SplineApiConfigModule } from 'spline-shared'
import { SplineAttributesSharedModule } from 'spline-shared/attributes'
import { SplineDynamicTableSharedModule } from 'spline-shared/dynamic-table'
import { SplineExpressionSharedModule } from 'spline-shared/expression'
import { SplineGraphSharedModule } from 'spline-shared/graph'
import { SplineTranslateModule } from 'spline-utils/translate'

import * as fromComponents from './components'
import * as fromPages from './pages'
import { SplineEventsRoutingModule } from './spline-events-routing.module'
import { EventOverviewStoreFacade } from './store'


@NgModule({
    declarations: [
        ...fromPages.pageComponents,
        ...fromComponents.components,
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        HttpClientModule,
        RouterModule,
        MatTableModule,
        MatSortModule,
        MatTooltipModule,
        MatDividerModule,
        MatPaginatorModule,
        MatCardModule,
        MatIconModule,
        MatTreeModule,
        MatButtonModule,
        MatTabsModule,
        MatInputModule,
        MatMenuModule,
        MatSlideToggleModule,
        SplineEventsRoutingModule,
        SplineApiConfigModule,
        SplineApiModule,
        SplineLayoutModule,
        SplineTranslateModule.forChild({
            moduleNames: ['events']
        }),
        SplineGraphModule,
        SplineAttributesSharedModule,
        SplineDataViewModule,
        SplineExpressionSharedModule,
        SplineCommonModule,
        SplineGraphSharedModule,
        DynamicTableModule,
        DynamicTableCommonCellsModule,
        SplineDynamicTableSharedModule,
        SplineListBoxModule,
        DynamicFilterModule,
        DfControlSelectModule,
        DfControlDateRangeModule
    ],
    exports: [
        ...fromPages.pageComponents,
    ],
    providers: [
        EventOverviewStoreFacade,
    ],
})
export class SplineEventsModule {
}
