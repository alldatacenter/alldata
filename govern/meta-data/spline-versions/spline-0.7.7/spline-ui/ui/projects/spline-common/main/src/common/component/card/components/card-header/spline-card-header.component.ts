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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core'
import { GenericEventInfo } from 'spline-utils'

import { SplineColors } from '../../../../models'
import { SplineCardHeader } from '../../models'


@Component({
    selector: 'spline-card-header',
    templateUrl: './spline-card-header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineCardHeaderComponent {

    @Input() headerTitle: string
    @Input() color = SplineColors.PINK // valid CSS color
    @Input() icon: string
    @Input() iconTooltip: string
    @Input() actions: SplineCardHeader.Action[]

    @Output() event$ = new EventEmitter<GenericEventInfo>()


    onActionClicked($event: MouseEvent, action: SplineCardHeader.Action): void {
        $event.stopPropagation()
        if (action?.onClick) {
            action.onClick()
        }

        if (action?.event) {
            this.event$.emit(action.event)
        }
    }
}
