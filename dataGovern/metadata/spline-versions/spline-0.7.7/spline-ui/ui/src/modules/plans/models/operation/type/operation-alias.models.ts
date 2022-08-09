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

import { OperationDetails, OperationPropertiesAlias } from 'spline-api'
import { SdWidgetExpansionPanel, SdWidgetSchema, SdWidgetSimpleRecord, SplineDataViewSchema } from 'spline-common/data-view'
import { SgNodeControl } from 'spline-shared/graph'

import { EventOperationProperty } from '../operation-property.models'

import { getBaseOperationDetailsSchema } from './operation-common.models'


export namespace OperationAlias {

    export function toDataViewSchema(operationDetails: OperationDetails): SplineDataViewSchema {

        return getBaseOperationDetailsSchema(
            operationDetails,
            getMainSection,
        )
    }

    export function getMainSection(operationDetails: OperationDetails,
                                   primitiveProps: EventOperationProperty.ExtraPropertyValuePrimitive[]): SdWidgetSchema[] {

        const properties = operationDetails.operation.properties as OperationPropertiesAlias
        const nodeStyles = SgNodeControl.getNodeStyles(SgNodeControl.NodeType.Alias)

        return [
            SdWidgetExpansionPanel.toSchema(
                {
                    title: 'PLANS.OPERATION__ALIAS__MAIN_SECTION_TITLE',
                    icon: nodeStyles.icon,
                    iconColor: nodeStyles.color,
                },
                [
                    SdWidgetSimpleRecord.toSchema({
                        label: 'PLANS.OPERATION__ALIAS__MAIN_SECTION__PROPERTY_NAME',
                        value: properties.name,
                    }),
                    ...EventOperationProperty.primitivePropsToDvs(primitiveProps),
                ]
            ),
        ]
    }

}
