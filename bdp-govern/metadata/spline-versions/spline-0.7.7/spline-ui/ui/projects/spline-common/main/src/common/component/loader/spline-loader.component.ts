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

import { Component, Input } from '@angular/core'

import { SplineLoader } from './spline-loader.models'


@Component({
    selector: 'spline-loader',
    templateUrl: './spline-loader.component.html',
})
export class SplineLoaderComponent {

    readonly defaultSize = SplineLoader.Size.lg
    readonly defaultMode = SplineLoader.Mode.floating

    readonly SPINNER_DIAMETER_MAP = SplineLoader.SPINNER_DIAMETER_MAP

    @Input() size: SplineLoader.Size = this.defaultSize
    @Input() mode: SplineLoader.Mode = this.defaultMode
    @Input() blurBackground: boolean

}
