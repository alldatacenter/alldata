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

import { dataSourceUriToName, OperationDetails, OperationPropertiesRead, uriToDatsSourceId } from 'spline-api'
import { SdWidgetExpansionPanel, SdWidgetRecordsList, SdWidgetSchema, SplineDataViewSchema } from 'spline-common/data-view'
import { SgNodeControl } from 'spline-shared/graph'

import { EventOperationProperty } from '../operation-property.models'

import { getBaseOperationDetailsSchema } from './operation-common.models'


export namespace OperationRead {

    export function toDataViewSchema(operationDetails: OperationDetails): SplineDataViewSchema {
        return getBaseOperationDetailsSchema(
            operationDetails,
            getMainSection,
            ['inputSources'],
        )
    }

    export function getMainSection(operationDetails: OperationDetails,
                                   primitiveProps: EventOperationProperty.ExtraPropertyValuePrimitive[]): SdWidgetSchema[] {

        const properties = operationDetails.operation.properties as OperationPropertiesRead
        const nodeStyles = SgNodeControl.getNodeStyles(SgNodeControl.NodeType.Read)

        return [
            SdWidgetExpansionPanel.toSchema(
                {
                    title: 'PLANS.OPERATION__READ__MAIN_SECTION_TITLE',
                    icon: nodeStyles.icon,
                    iconColor: nodeStyles.color,
                },
                // INPUT DATA SOURCES & EXTRA PROPS :: PRIMITIVE
                [
                    SdWidgetRecordsList.toSchema(
                        properties.inputSources
                            .map(uri => ({
                                value: dataSourceUriToName(uri),
                                description: uri,
                                routerLink: ['/data-sources/overview', uriToDatsSourceId(uri)]
                            })),
                        'PLANS.OPERATION__READ__INPUT_DATA_SOURCES_TITLE',
                    ),
                    ...EventOperationProperty.primitivePropsToDvs(primitiveProps),
                ],
            ),
        ]
    }

}
