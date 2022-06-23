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
import { MatButtonModule } from '@angular/material/button'
import { MatCardModule } from '@angular/material/card'
import { MatDividerModule } from '@angular/material/divider'
import { MatIconModule } from '@angular/material/icon'
import { MatTooltipModule } from '@angular/material/tooltip'
import { NgxGraphModule } from '@swimlane/ngx-graph'
import { SplineCommonModule } from 'spline-common'
import { SplineTranslateModule } from 'spline-utils/translate'


import * as fromComponents from './components'
import * as fromDirectives from './directives'
import * as fromServices from './services'


@NgModule({
    declarations: [
        ...fromComponents.splineGraphComponents,
        ...fromDirectives.splineGraphDirectives
    ],
    imports: [
        CommonModule,
        NgxGraphModule,
        MatIconModule,
        MatButtonModule,
        MatDividerModule,
        MatTooltipModule,
        MatCardModule,
        SplineTranslateModule,
        SplineCommonModule,
    ],
    exports: [
        ...fromComponents.splineGraphComponents,
        ...fromDirectives.splineGraphDirectives,
    ],
    providers: [
        ...fromServices.splineGraphServices
    ]
})
export class SplineGraphModule {
}
