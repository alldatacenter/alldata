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
import { MultiTranslateHttpLoader } from 'ngx-translate-multi-http-loader'
import { StringHelpers, TypeHelpers } from 'spline-utils'


export function TranslateLoaderFactory(http: HttpClient, assets: string[], rootAssets: string[] = []): MultiTranslateHttpLoader {
    const loaderAssets = [...assets, ...rootAssets]
        .map(
            asset => ({
                prefix: asset,
                suffix: '.json?cache=' + StringHelpers.guid(),
            }),
        )
    return new MultiTranslateHttpLoader(http, loaderAssets)
}

export interface SplineTranslateRootConfig {
    defaultLang?: string
    commonAssets?: string[]
}

export interface SplineTranslateChildConfig {
    moduleNames?: string | string[]
}

// TODO: move base href calculation to some shared helper
const BASE_HREF = document?.getElementsByTagName('base')[0]?.attributes['href']?.value || '/'
export const ASSETS_BASE_PATH = `${BASE_HREF}assets/i18n`

export function toAssetsFilePath(moduleNames: string | string[]): string[] {
    if (!TypeHelpers.isArray(moduleNames)) {
        moduleNames = [moduleNames]
    }
    return moduleNames.map(currentModuleName => `${ASSETS_BASE_PATH}/${currentModuleName}/`)
}

export const COMMON_ASSETS = [toAssetsFilePath('common')]

export const DEFAULT_LANG = 'en'
export const DEFAULT_CONFIG: SplineTranslateRootConfig = { defaultLang: DEFAULT_LANG }

export function ModuleAssetsFactory(config: SplineTranslateChildConfig): string[] {
    return config.moduleNames ? toAssetsFilePath(config.moduleNames) : []
}
