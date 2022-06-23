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
import { Inject, InjectionToken, ModuleWithProviders, NgModule, Optional } from '@angular/core'
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core'
import { SplineUtilsCommonModule } from 'spline-utils'

import { SPLINE_TRANSLATE_COMMON_ASSETS, SPLINE_TRANSLATE_CONFIG } from './spline-translate-core.module'
import {
    ModuleAssetsFactory,
    SplineTranslateChildConfig,
    SplineTranslateRootConfig,
    TranslateLoaderFactory,
} from './spline-translate.models'


export const SPLINE_TRANSLATE_ASSETS = new InjectionToken<string[]>('SPLINE_TRANSLATE_ASSETS')
export const SPLINE_TRANSLATE_CHILD_CONFIG = new InjectionToken<SplineTranslateChildConfig>('SPLINE_TRANSLATE_CHILD_CONFIG')


@NgModule({
    imports: [
        SplineUtilsCommonModule,
        TranslateModule.forChild({
            useDefaultLang: true,
            loader: {
                provide: TranslateLoader,
                useFactory: TranslateLoaderFactory,
                deps: [HttpClient, SPLINE_TRANSLATE_ASSETS, SPLINE_TRANSLATE_COMMON_ASSETS],
            },
            isolate: true,
        }),
    ],
    exports: [
        TranslateModule,
    ],
})
export class SplineTranslateModule {

    constructor(
        @Optional() @Inject(SPLINE_TRANSLATE_CONFIG) config: SplineTranslateRootConfig, translate: TranslateService,
    ) {
        if (!config) {
            throw new Error(
                'SPLINE_TRANSLATE_CONFIG provider does not exist. Please inject SplineTranslateCoreModule into your root module.',
            )
        }

        translate.setDefaultLang(config.defaultLang)
    }

    static forChild(config: SplineTranslateChildConfig = {}): ModuleWithProviders<SplineTranslateModule> {
        return {
            ngModule: SplineTranslateModule,
            providers: [
                {
                    provide: SPLINE_TRANSLATE_ASSETS,
                    useFactory: ModuleAssetsFactory,
                    deps: [SPLINE_TRANSLATE_CHILD_CONFIG],
                },
                {
                    provide: SPLINE_TRANSLATE_CHILD_CONFIG,
                    useValue: config,
                },
            ],
        }
    }
}
