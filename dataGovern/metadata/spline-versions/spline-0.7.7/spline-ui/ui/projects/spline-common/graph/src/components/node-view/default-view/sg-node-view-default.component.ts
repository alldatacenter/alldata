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
import { SplineColors } from 'spline-common'
import { BaseComponent } from 'spline-utils'

import { SgNodeViewDefault } from './sg-node-view-default.models'


@Component({
    selector: 'sg-node-view-default',
    templateUrl: './sg-node-view-default.component.html',
})
export class SgNodeViewDefaultComponent extends BaseComponent {

    readonly defaultIcon = 'extension'
    readonly defaultColor = SplineColors.SILVER
    readonly defaultActionsPosition = SgNodeViewDefault.DEFAULT_POSITION

    @Input() disallowSelection: boolean
    @Input() isSelected: boolean
    @Input() isFocused: boolean
    @Input() isTarget: boolean
    @Input() icon: string
    @Input() color: string
    @Input() showActions = false
    @Input() actionsPosition: SgNodeViewDefault.ActionsPosition = this.defaultActionsPosition

}
