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

import { HttpClient } from '@angular/common/http'
import { InjectionToken, NgModule } from '@angular/core'
import { TranslateLoader, TranslateModule } from '@ngx-translate/core'

import { COMMON_ASSETS, DEFAULT_CONFIG, SplineTranslateRootConfig, TranslateLoaderFactory } from './spline-translate.models'


export const SPLINE_TRANSLATE_COMMON_ASSETS = new InjectionToken<string[]>('SPLINE_TRANSLATE_COMMON_ASSETS')
export const SPLINE_TRANSLATE_CONFIG = new InjectionToken<SplineTranslateRootConfig>('SPLINE_TRANSLATE_CONFIG')


@NgModule({
    imports: [
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: TranslateLoaderFactory,
                deps: [HttpClient, SPLINE_TRANSLATE_COMMON_ASSETS],
            },
        }),
    ],
    providers: [
        {
            provide: SPLINE_TRANSLATE_COMMON_ASSETS,
            useValue: COMMON_ASSETS,
        },
        {
            provide: SPLINE_TRANSLATE_CONFIG,
            useValue: DEFAULT_CONFIG,
        },
    ],
    exports: [
        TranslateModule,
    ],
})
export class SplineTranslateCoreModule {

}
