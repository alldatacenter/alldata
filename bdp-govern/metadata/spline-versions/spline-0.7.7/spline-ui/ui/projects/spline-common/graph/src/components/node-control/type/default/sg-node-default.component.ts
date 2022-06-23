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

import { Component } from '@angular/core'

import { SgNodeBaseComponent } from '../sg-node-base.component'

import { SgNodeDefault } from './sg-node-default.models'
import InlineAction = SgNodeDefault.InlineAction
import Action = SgNodeDefault.Action;


@Component({
    selector: 'sg-node-default',
    templateUrl: './sg-node-default.component.html',
})
export class SgNodeDefaultComponent extends SgNodeBaseComponent<SgNodeDefault.Data, SgNodeDefault.Options> {

    onActionClicked($event: MouseEvent, action: InlineAction | Action): void {
        $event.stopPropagation()
        if (action?.onClick) {
            action.onClick()
        }

        if (action?.event) {
            this.event$.emit(action.event)
        }
    }
}
