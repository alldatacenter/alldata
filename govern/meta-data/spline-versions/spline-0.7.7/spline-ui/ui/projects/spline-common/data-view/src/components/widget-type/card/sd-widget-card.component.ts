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

import { ChangeDetectionStrategy, Component } from '@angular/core'
import { GenericEventInfo } from 'spline-utils'

import { SplineDataWidgetEvent } from '../../../models'
import { SdWidgetBaseComponent } from '../sd-widget-base.component'

import { SdWidgetCard } from './sd-widget-card.models'


@Component({
    selector: 'sd-widget-card',
    templateUrl: './sd-widget-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SdWidgetCardComponent extends SdWidgetBaseComponent<SdWidgetCard.Data> {

    onWidgetEvent($event: SplineDataWidgetEvent): void {
        this.event$.emit($event)
    }

    onCardHeaderEvent($event: GenericEventInfo): void {
        this.event$.emit($event)
    }
}
