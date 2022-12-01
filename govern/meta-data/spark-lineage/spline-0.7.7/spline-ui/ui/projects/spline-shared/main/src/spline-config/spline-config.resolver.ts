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

import { Injectable } from '@angular/core'
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router'
import { Observable, of } from 'rxjs'
import { tap } from 'rxjs/operators'

import { hasQueryParamsSplineConfig, SplineConfig } from './spline-config.models'
import { SplineConfigService } from './spline-config.service'


@Injectable({
    providedIn: 'root',
})
export class SplineConfigResolver implements Resolve<SplineConfig> {

    constructor(private readonly splineConfigService: SplineConfigService,
                private readonly router: Router) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<SplineConfig> {

        return this.splineConfigService.config !== null
            ? of(this.splineConfigService.config)
            : this.splineConfigService.initConfig(route.queryParamMap)
                .pipe(
                    tap(config => {
                        if (hasQueryParamsSplineConfig(route.queryParamMap)) {
                            void this.router.navigateByUrl(config?.targetUrl ?? '/')
                        }
                    })
                )
    }

}
