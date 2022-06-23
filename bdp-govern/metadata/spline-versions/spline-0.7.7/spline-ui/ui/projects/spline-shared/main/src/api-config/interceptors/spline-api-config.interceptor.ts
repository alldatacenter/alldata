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

import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http'
import { Injectable } from '@angular/core'
import { Observable } from 'rxjs'

import { SplineConfigService } from '../../spline-config/spline-config.service'
import { SplineApiConfig } from '../models/spline-api-config.models'


@Injectable()
export class SplineApiConfigInterceptor implements HttpInterceptor {

    constructor(private readonly splineConfigService: SplineConfigService) {
    }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        if (SplineApiConfig.hasUrlAnyApiAlias(request.url)) {
            const splineConfig = this.splineConfigService.config
            request = request.clone({
                url: SplineApiConfig.decorateUrl(request.url, splineConfig),
            })
            return next.handle(request)

        }
        else {
            return next.handle(request)
        }
    }
}
