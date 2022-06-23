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

import { HttpClient, HttpHeaders } from '@angular/common/http'
import { Inject, Injectable, Optional } from '@angular/core'
import { ParamMap } from '@angular/router'
import { BehaviorSubject, combineLatest, Observable, of, throwError } from 'rxjs'
import { catchError, filter, first, map, tap } from 'rxjs/operators'

import {
    hasQueryParamsSplineConfig,
    initSplineConfigFromQueryParams,
    SPLINE_CONFIG_SETTINGS,
    SplineConfig,
    SplineConfigSettings
} from './spline-config.models'


@Injectable({
    providedIn: 'root',
})
export class SplineConfigService {

    config$: Observable<SplineConfig>

    get config(): SplineConfig | null {
        return this._config$.getValue()
    }

    private _config$ = new BehaviorSubject<SplineConfig>(null)

    constructor(private readonly http: HttpClient,
                @Optional() @Inject(SPLINE_CONFIG_SETTINGS) private readonly settings: SplineConfigSettings) {

        this.config$ = this._config$
            .pipe(
                filter(x => !!x),
            )

        if (!this.settings) {
            throw new Error('SPLINE_CONFIG_SETTINGS is required.')
        }
    }

    initConfig(queryParamMap?: ParamMap): Observable<SplineConfig> {
        // first check if query config is in query params, if not fetch from url.
        const userConfig$ = queryParamMap && hasQueryParamsSplineConfig(queryParamMap)
            ? SplineConfigService.loadUserConfigFromUrlParams(queryParamMap)
            : this.loadUserConfigFromAssets()

        const defaultConfig$ = this.fetchConfig(this.settings.defaultConfigUri)

        return combineLatest([defaultConfig$, userConfig$]).pipe(
            map(([defaultConfig, userConfig]) => ({
                ...defaultConfig,
                ...userConfig
            })),
            tap(config => this._config$.next(config))
        )
    }

    private static loadUserConfigFromUrlParams(queryParamMap: ParamMap): Observable<SplineConfig> {
        return of(initSplineConfigFromQueryParams(queryParamMap))
    }

    private loadUserConfigFromAssets(): Observable<SplineConfig> {
        return this.fetchConfig(this.settings.userConfigUri)
            .pipe(
                catchError((error) => {
                    console.error(`
                        The Spline App Config file cannot be found.
                        Please make sure the file exists: ${this.settings.userConfigUri}.
                    `)
                    return throwError(error)
                }),
                first()
            )
    }

    private fetchConfig(url: string): Observable<SplineConfig> {
        return this.http.get<SplineConfig>(
            url,
            {
                headers: new HttpHeaders({
                    'Cache-Control': 'no-cache, no-store, must-revalidate',
                })
            }
        )
    }
}
