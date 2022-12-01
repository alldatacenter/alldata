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

import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core'
import { SdWidgetCard, SplineDataViewSchema } from 'spline-common/data-view'
import { SdWidgetAttributesTree, SplineAttributesTree } from 'spline-shared/attributes'
import { SgNodeControl } from 'spline-shared/graph'
import { BaseLocalStateComponent } from 'spline-utils'

import { ExecutionPlanInfoStore } from '../../../store'


@Component({
    selector: 'plan-attribute-info',
    templateUrl: './attribute-info.component.html',
    styleUrls: ['./attribute-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttributeInfoComponent extends BaseLocalStateComponent<ExecutionPlanInfoStore.State> implements OnChanges {

    @Input() attributeTreeSchema: SplineAttributesTree.Tree

    attributeTreeDvs: SplineDataViewSchema
    attributeInfoDvs: SplineDataViewSchema

    constructor() {
        super()
    }

    ngOnChanges(changes: SimpleChanges): void {
        const {attributeTreeSchema} = changes

        if (attributeTreeSchema && attributeTreeSchema.currentValue) {
            this.attributeTreeDvs = SdWidgetCard.toContentOnlySchema([
                SdWidgetAttributesTree.toSchema(
                    attributeTreeSchema.currentValue,
                    {
                        allowAttrSelection: false,
                        allowSearch: false,
                        expandAll: true
                    }
                )
            ])

            const nodeStyles = SgNodeControl.getNodeStyles(SgNodeControl.NodeType.Attribute)
            this.attributeInfoDvs = SdWidgetCard.toSchema(
                {
                    color: nodeStyles.color,
                    icon: nodeStyles.icon,
                    title: attributeTreeSchema.currentValue[0].name,
                }
            )
        }
    }



}
