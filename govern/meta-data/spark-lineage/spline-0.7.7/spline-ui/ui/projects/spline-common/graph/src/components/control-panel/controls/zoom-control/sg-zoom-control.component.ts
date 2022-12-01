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

import { Component, EventEmitter, Input, Output } from '@angular/core'


@Component({
    selector: 'sg-zoom-control',
    templateUrl: './sg-zoom-control.component.html',
})
export class SgZoomControlComponent {

    @Input() zoomLevel = 1

    @Output() zoomIn$ = new EventEmitter<void>()
    @Output() zoomOut$ = new EventEmitter<void>()

    onZoomInBtnClicked(): void {
        this.zoomIn$.next()
    }

    onZoomOutBtnClicked(): void {
        this.zoomOut$.next()
    }

}
