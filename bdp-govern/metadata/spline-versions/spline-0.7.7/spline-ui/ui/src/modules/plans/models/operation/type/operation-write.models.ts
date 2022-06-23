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

import { dataSourceUriToName, OperationDetails, OperationPropertiesWrite, uriToDatsSourceId } from 'spline-api'
import { SdWidgetExpansionPanel, SdWidgetRecordsList, SdWidgetSchema, SplineDataViewSchema } from 'spline-common/data-view'
import { SgNodeControl } from 'spline-shared/graph'

import { EventOperationProperty } from '../operation-property.models'

import { getBaseOperationDetailsSchema } from './operation-common.models'


export namespace OperationWrite {

    export function toDataViewSchema(operationDetails: OperationDetails): SplineDataViewSchema {

        return getBaseOperationDetailsSchema(
            operationDetails,
            getMainSection,
            ['outputSource'],
        )
    }

    export function getMainSection(operationDetails: OperationDetails,
                                   primitiveProps: EventOperationProperty.ExtraPropertyValuePrimitive[]): SdWidgetSchema[] {

        const properties = operationDetails.operation.properties as OperationPropertiesWrite
        const nodeStyles = SgNodeControl.getNodeStyles(SgNodeControl.NodeType.Write)

        return [
            SdWidgetExpansionPanel.toSchema(
                {
                    title: 'PLANS.OPERATION__WRITE__MAIN_SECTION_TITLE',
                    icon: nodeStyles.icon,
                    iconColor: nodeStyles.color,
                },
                // OUTPUT DATA SOURCE & EXTRA PROPS :: PRIMITIVE
                [
                    SdWidgetRecordsList.toSchema(
                        [
                            {
                                value: dataSourceUriToName(properties.outputSource),
                                description: properties.outputSource,
                                routerLink: ['/data-sources/overview', uriToDatsSourceId(properties.outputSource)]
                            },
                        ],
                        'PLANS.OPERATION__WRITE__OUTPUT_DATA_SOURCE_TITLE',
                    ),
                    ...EventOperationProperty.primitivePropsToDvs(primitiveProps),
                ],
            ),
        ]
    }

}
