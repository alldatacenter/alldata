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

import { Component } from '@angular/core'

import { SgNodeBaseComponent } from '../sg-node-base.component'

import { SgNodeCircleButton } from './sg-node-circle-button.models'


@Component({
    selector: 'sg-node-circle-button',
    templateUrl: './sg-node-circle-button.component.html',
})
export class SgNodeCircleButtonComponent extends SgNodeBaseComponent<SgNodeCircleButton.Data, SgNodeCircleButton.Options> {

    onButtonClicked(): void {
        this.event$.emit({
            type: this.data.eventName
        })
    }
}
