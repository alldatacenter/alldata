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
    selector: 'sg-depth-control',
    templateUrl: './sg-depth-control.component.html',
})
export class SgDepthControlComponent {

    @Input() depth

    @Output() depthChanged$ = new EventEmitter<{ depth: number }>()

    onShowMoreBtnClicked(): void {
        this.depthChanged$.emit({ depth: this.depth * 2 })
    }
}
