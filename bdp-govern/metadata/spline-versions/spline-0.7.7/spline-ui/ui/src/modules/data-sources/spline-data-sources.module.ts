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
import { HttpClientModule } from '@angular/common/http'
import { NgModule } from '@angular/core'
import { MatButtonModule } from '@angular/material/button'
import { MatCardModule } from '@angular/material/card'
import { MatDividerModule } from '@angular/material/divider'
import { MatIconModule } from '@angular/material/icon'
import { MatInputModule } from '@angular/material/input'
import { MatMenuModule } from '@angular/material/menu'
import { MatPaginatorModule } from '@angular/material/paginator'
import { MatSortModule } from '@angular/material/sort'
import { MatTableModule } from '@angular/material/table'
import { MatTabsModule } from '@angular/material/tabs'
import { MatTooltipModule } from '@angular/material/tooltip'
import { MatTreeModule } from '@angular/material/tree'
import { RouterModule } from '@angular/router'
import { EffectsModule } from '@ngrx/effects'
import { StoreModule } from '@ngrx/store'
import { NgxDaterangepickerMd } from 'ngx-daterangepicker-material'
import { SplineApiModule } from 'spline-api'
import { SplineCommonModule } from 'spline-common'
import { SplineDataViewModule } from 'spline-common/data-view'
import { DynamicFilterModule } from 'spline-common/dynamic-filter'
import { DfControlDateRangeModule, DfControlSelectModule } from 'spline-common/dynamic-filter/filter-controls'
import { DynamicTableCommonCellsModule, DynamicTableModule } from 'spline-common/dynamic-table'
import { SplineLayoutModule } from 'spline-common/layout'
import { SplineApiConfigModule } from 'spline-shared'
import { SplineDynamicTableSharedModule } from 'spline-shared/dynamic-table'
import { SplineEventsSharedModule } from 'spline-shared/events'
import { SplineTranslateModule } from 'spline-utils/translate'

import { components } from './components'
import * as fromPages from './pages'
import { services } from './services'
import { SplineDataSourcesRoutingModule } from './spline-data-sources-routing.module'
import { effects } from './store'
import { SplineDataSourceStore } from './store/reducers/base'


@NgModule({
    declarations: [
        ...fromPages.pageComponents,
        ...components,
    ],
    imports: [
        CommonModule,
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
        NgxDaterangepickerMd.forRoot(),
        StoreModule.forFeature(SplineDataSourceStore.STORE_FEATURE_NAME, SplineDataSourceStore.reducers),
        EffectsModule.forFeature(effects),
        SplineDataSourcesRoutingModule,
        SplineApiConfigModule,
        SplineDataViewModule,
        SplineApiModule,
        SplineLayoutModule,
        SplineTranslateModule.forChild({ moduleNames: ['data-sources'] }),
        SplineCommonModule,
        SplineEventsSharedModule,
        DynamicTableModule,
        DynamicTableCommonCellsModule,
        SplineDynamicTableSharedModule,
        ScrollingModule,
        DynamicFilterModule,
        DfControlSelectModule,
        DfControlDateRangeModule
    ],
    exports: [
        ...fromPages.pageComponents,
    ],
    providers: [
        ...services
    ],
})
export class SplineDataSourcesModule {
}
