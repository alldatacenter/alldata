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
import { MatChipsModule } from '@angular/material/chips'
import { MatDialogModule } from '@angular/material/dialog'
import { MatDividerModule } from '@angular/material/divider'
import { MatIconModule } from '@angular/material/icon'
import { MatTooltipModule } from '@angular/material/tooltip'
import { MatTreeModule } from '@angular/material/tree'
import { SD_WIDGET_FACTORY } from 'spline-common/data-view'
import { SplineTranslateModule } from 'spline-utils/translate'

import * as fromComponents from './components'
import { SdWidgetExpressionFactory } from './services'


@NgModule({
    imports: [
        CommonModule,
        MatCardModule,
        MatTreeModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        MatChipsModule,
        MatDialogModule,
        MatDividerModule,
        SplineTranslateModule,
    ],
    declarations: [
        ...fromComponents.attributesTreeComponents,
    ],
    exports: [
        ...fromComponents.attributesTreeComponents,
    ],
    providers: [
        SdWidgetExpressionFactory,
        {
            provide: SD_WIDGET_FACTORY,
            useValue: SdWidgetExpressionFactory,
            multi: true,
        },
    ],
})
export class SplineExpressionSharedModule {
}
