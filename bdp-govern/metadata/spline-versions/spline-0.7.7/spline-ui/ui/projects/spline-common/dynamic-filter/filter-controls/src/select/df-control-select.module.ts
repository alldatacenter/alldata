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
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { SplineInlineFilterModule, SplineListBoxModule } from 'spline-common'
import { DF_CONTROL_FACTORY, DynamicFilterControlManager, DynamicFilterModule } from 'spline-common/dynamic-filter'
import { SplineTranslateModule } from 'spline-utils/translate'

import { DfControlSelectComponent } from './components'
import { DfControlSelectFactory } from './services/df-control-select.factory'


@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,

        SplineTranslateModule.forChild(),
        DynamicFilterModule,
        SplineInlineFilterModule,
        SplineListBoxModule
    ],
    declarations: [
        DfControlSelectComponent
    ],
    exports: [
        DfControlSelectComponent
    ],
    providers: [
        DfControlSelectFactory,
        {
            provide: DF_CONTROL_FACTORY,
            useValue: DfControlSelectFactory,
            multi: true
        }
    ]
})
export class DfControlSelectModule {
    constructor(private readonly dynamicFilterControlManager: DynamicFilterControlManager,
                private readonly dfControlSelectFactory: DfControlSelectFactory) {

        this.dynamicFilterControlManager.registerStaticFactory(
            this.dfControlSelectFactory
        )
    }
}
