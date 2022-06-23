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

import { Component, EventEmitter, Input, Output } from '@angular/core'
import { ThemePalette } from '@angular/material/core/common-behaviors/color'
import { BaseComponent } from 'spline-utils'


@Component({
    selector: 'sg-node-view-circle-button',
    templateUrl: './sg-node-view-circle-button.component.html',
})
export class SgNodeViewCircleButtonComponent extends BaseComponent {

    readonly defaultIcon = 'history'
    readonly defaultColor = 'accent'

    @Input() isSelected: boolean
    @Input() isFocused: boolean
    @Input() isTarget: boolean

    @Input() color: ThemePalette
    @Input() icon: string
    @Input() tooltip: string

    @Output() buttonClicked$ = new EventEmitter<void>()

    onButtonClicked($event: MouseEvent): void {
        $event.preventDefault()
        $event.stopPropagation()
        this.buttonClicked$.emit()
    }
}
