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

import { InjectionToken } from '@angular/core'
import { ParamMap } from '@angular/router'


export type SplineConfig = {
    splineConsumerApiUrl: string
    isEmbeddedMode: boolean
    targetUrl?: string
    serverPollingIntervalMs: number
}

export type SplineConfigSettings = {
    defaultConfigUri: string
    userConfigUri: string
}

export const SPLINE_CONFIG_SETTINGS = new InjectionToken<SplineConfigSettings>('SPLINE_CONFIG_SETTINGS')

export enum SplineConfigQueryParam {
    splineConsumerApiUrl = '_splineConsumerApiUrl', // required
    isEmbeddedMode = '_isEmbeddedMode',
    targetUrl = '_targetUrl'
}

export function hasQueryParamsSplineConfig(paramsMap: ParamMap): boolean {
    return paramsMap.has(SplineConfigQueryParam.splineConsumerApiUrl)
}

export function initSplineConfigFromQueryParams(paramsMap: ParamMap): SplineConfig {
    return Object.keys(SplineConfigQueryParam)
        .reduce(
            (acc, key) => {
                const queryParamName = SplineConfigQueryParam[key]

                if (paramsMap.has(queryParamName)) {
                    return { ...acc, [key]: paramsMap.get(queryParamName) }
                }

                return { ...acc }

            },
            {} as SplineConfig
        )
}
