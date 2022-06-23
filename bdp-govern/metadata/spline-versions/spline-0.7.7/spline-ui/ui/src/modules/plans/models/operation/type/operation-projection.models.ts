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

import { AttributeSchema, OperationDetails, OperationPropertiesProjection } from 'spline-api'
import { SplineColors } from 'spline-common'
import { SdWidgetExpansionPanel, SdWidgetRecordsList, SdWidgetSchema, SplineDataViewSchema } from 'spline-common/data-view'
import { SdWidgetExpression } from 'spline-shared/expression'
import { SgNodeControl } from 'spline-shared/graph'

import { EventOperationProperty } from '../operation-property.models'

import { getBaseOperationDetailsSchema } from './operation-common.models'


export namespace OperationProjection {

    export function toDataViewSchema(operationDetails: OperationDetails): SplineDataViewSchema {

        return getBaseOperationDetailsSchema(
            operationDetails,
            getMainSection,
            ['projectList'],
        )
    }

    export function getMainSection(operationDetails: OperationDetails,
                                   primitiveProps: EventOperationProperty.ExtraPropertyValuePrimitive[]): SdWidgetSchema[] {

        const nodeStyles = SgNodeControl.getNodeStyles(SgNodeControl.NodeType.Projection)
        const properties = operationDetails.operation.properties as OperationPropertiesProjection
        const mainSectionSchema = primitiveProps.length
            ? [
                SdWidgetExpansionPanel.toSchema(
                    {
                        title: 'PLANS.OPERATION__PROJECTION__MAIN_SECTION_TITLE',
                        icon: nodeStyles.icon,
                        iconColor: nodeStyles.color,
                    },
                    [

                        ...EventOperationProperty.primitivePropsToDvs(primitiveProps),
                    ],
                ),
            ]
            : []

        const transformationsSchema = [
            SdWidgetExpansionPanel.toSchema(
                {
                    title: 'PLANS.OPERATION__PROJECTION__TRANSFORMATIONS_TITLE',
                    icon: nodeStyles.icon,
                    iconColor: nodeStyles.color,
                },
                properties.projectList.map(
                    expression => SdWidgetExpression.toSchema({
                        expression,
                        attrSchemasCollection: operationDetails.schemasCollection,
                    }),
                ),
            ),
        ]

        const droppedAttributesSchema = getDroppedAttributesDvs(operationDetails)

        return [...mainSectionSchema, ...transformationsSchema, ...droppedAttributesSchema]
    }

    export function getDroppedAttributesDvs(operationDetails: OperationDetails): SdWidgetSchema[] {

        const droppedAttributes = extractDroppedAttributes(operationDetails)
        return droppedAttributes.length
            ? [
                SdWidgetExpansionPanel.toSchema(
                    {
                        title: 'PLANS.OPERATION__PROJECTION__DROPPED_ATTRIBUTES_TITLE',
                        icon: 'label_off',
                        iconColor: SplineColors.PINK,
                    },
                    [
                        SdWidgetRecordsList.toSchema(
                            droppedAttributes.map(attr => ({
                                value: attr.name,
                            })),
                        ),
                    ],
                ),
            ]
            : []

    }

    function extractDroppedAttributes(operationDetails: OperationDetails): AttributeSchema[] {
        const inputAttrsIds = operationDetails.inputs
            .reduce(
                (acc, index) => {
                    const currentInputAttrsIds = operationDetails.schemas[index].map(item => item.id)
                    return [...acc, ...currentInputAttrsIds]
                },
                [],
            )

        const outputAttrIdsMap = new Set(operationDetails.schemas[operationDetails.output].map(item => item.id))

        return inputAttrsIds
            .filter(id => !outputAttrIdsMap.has(id))
            .map(id => operationDetails.schemasCollection[id])
    }

}
