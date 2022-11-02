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

import { SplineConsumerApiSettings } from 'spline-api'

import { SplineConfig } from '../../spline-config'


export namespace SplineApiConfig {

    export const API_BASE_PATH_MAP = [
        SplineConsumerApiSettings.API_URL_PREFIX_ALIAS
    ]

    export function getApiBasePath(alias: string, splineConfig: SplineConfig): string {
        switch (alias) {

            case SplineConsumerApiSettings.API_URL_PREFIX_ALIAS:
                return splineConfig.splineConsumerApiUrl

            default:
                return ''
        }
    }

    export function hasUrlAnyApiAlias(url): boolean {
        for (const alias of API_BASE_PATH_MAP) {
            if (url.indexOf(alias) === 0) {
                return true
            }
        }
        return false
    }

    export function decorateUrl(url, splineConfig: SplineConfig): string {
        for (const alias of API_BASE_PATH_MAP) {
            if (url.indexOf(alias) === 0) {
                return url.replace(alias, getApiBasePath(alias, splineConfig))
            }
        }
        return url
    }
}


