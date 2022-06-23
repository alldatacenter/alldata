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

import { ActivatedRoute, NavigationExtras, Params, Router } from '@angular/router'
import { omit } from 'lodash-es'

import { TypeHelpers } from './heplers/type-helpers'


export namespace RouterNavigation {

    export type NavigationInfo = {
        path: string | any[]
        extras?: NavigationExtras
    }

    export function appendPath(navigationDetails: NavigationInfo, path: any[]): NavigationInfo {
        return {
            ...navigationDetails,
            path: [...navigationDetails.path, ...path],
        }
    }

    export function appendQueryParams(navigationDetails: NavigationInfo, extraQueryParams: Record<string, any>): NavigationInfo {
        return {
            ...navigationDetails,
            extras: {
                ...navigationDetails.extras,
                queryParams: {
                    ...(navigationDetails.extras?.queryParams ?? {}),
                    ...extraQueryParams
                }
            }
        }
    }

    export function extractQueryParam(activatedRoute: ActivatedRoute, queryParamName: string): string | null {
        return activatedRoute.snapshot.queryParamMap.get(queryParamName)
    }

    export function navigate(router: Router, navigationDetails: NavigationInfo): Promise<boolean> {
        const command = TypeHelpers.isArray(navigationDetails.path)
            ? navigationDetails.path
            : [navigationDetails.path]

        return router.navigate(command, navigationDetails?.extras)
    }

    export function updateCurrentRouterQueryParams(router: Router,
                                                   activatedRoute: ActivatedRoute,
                                                   queryParams: Params,
                                                   replaceUrl: boolean = true): void {

        const extras: NavigationExtras = {
            queryParams,
            relativeTo: activatedRoute,
            replaceUrl,
        }

        router.navigate([], extras)
    }

    export function setQueryParam(queryParams: Params, paramName: string, value: string | null): Params {
        return value
            ? {
                ...queryParams,
                [paramName]: value,
            }
            : omit(queryParams, paramName)
    }

    export function updateCurrentRouterOneQueryParam(router: Router,
                                                     activatedRoute: ActivatedRoute,
                                                     paramName: string,
                                                     value: string | null,
                                                     replaceUrl: boolean = true): void {

        const queryParams = setQueryParam(activatedRoute.snapshot.queryParams, paramName, value)
        updateCurrentRouterQueryParams(router, activatedRoute, queryParams, replaceUrl)
    }

}
