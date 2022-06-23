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

import { Component, EventEmitter, Output } from '@angular/core'
import { Observable } from 'rxjs'
import { map } from 'rxjs/operators'
import { SdWidgetBaseComponent } from 'spline-common/data-view'

import { SdWidgetAttributesTree, SplineAttributesTree } from '../../models'


@Component({
    selector: 'sd-widget-attributes-tree',
    templateUrl: './sd-widget-attributes-tree.component.html',
})
export class SdWidgetAttributesTreeComponent extends SdWidgetBaseComponent<SdWidgetAttributesTree.Data, SdWidgetAttributesTree.Options> {

    selectedAttributeId$: Observable<string | null>
    attributesTree$: Observable<SplineAttributesTree.Tree | null>

    constructor() {
        super()

        this.selectedAttributeId$ = this.options$
            .pipe(
                map(options => options?.selectedAttributeId ?? null),
            )

        this.attributesTree$ = this.data$
            .pipe(
                map(data => data?.attributesTree ?? null),
            )
    }

    onSelectedAttrChanged($event: { attributeId: string }): void {
        if (this.options?.allowAttrSelection !== false) {
            const eventObj = SdWidgetAttributesTree.createEventSelectedAttrChanged($event.attributeId)
            this.event$.emit(eventObj)
        }
    }
}
