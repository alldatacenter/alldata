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
import { MatButtonModule } from '@angular/material/button'
import { MatButtonToggleModule } from '@angular/material/button-toggle'
import { MatCardModule } from '@angular/material/card'
import { MatChipsModule } from '@angular/material/chips'
import { MatDialogModule } from '@angular/material/dialog'
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
import { NgxDaterangepickerMd } from 'ngx-daterangepicker-material'
import { SplineApiModule } from 'spline-api'
import { SplineCommonModule } from 'spline-common'
import { SplineDataViewModule } from 'spline-common/data-view'
import { SplineGraphModule } from 'spline-common/graph'
import { SplineLayoutModule } from 'spline-common/layout'
import { SplineApiConfigModule } from 'spline-shared'
import { SplineAttributesSharedModule } from 'spline-shared/attributes'
import { SplineExpressionSharedModule } from 'spline-shared/expression'
import { SplineGraphSharedModule } from 'spline-shared/graph'
import { SplineTranslateModule } from 'spline-utils/translate'


import * as fromComponents from './components'
import * as fromDirectives from './directives'
import * as fromPages from './pages'
import { SplinePlansRoutingModule } from './spline-plans-routing.module'


@NgModule({
    declarations: [
        ...fromPages.pageComponents,
        ...fromComponents.components,
        ...fromDirectives.directives,
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
        MatChipsModule,
        NgxDaterangepickerMd.forRoot(),
        SplinePlansRoutingModule,
        SplineApiConfigModule,
        SplineApiModule,
        SplineLayoutModule,
        SplineTranslateModule.forChild({
            moduleNames: [
                'plans',
            ]
        }),
        SplineGraphModule,
        SplineAttributesSharedModule,
        SplineDataViewModule,
        SplineExpressionSharedModule,
        SplineCommonModule,
        SplineGraphSharedModule,
        MatButtonToggleModule,
        MatSlideToggleModule,
        MatDialogModule
    ],
    exports: [
        ...fromPages.pageComponents,
    ],
    providers: [],
})
export class SplinePlansModule {
}
