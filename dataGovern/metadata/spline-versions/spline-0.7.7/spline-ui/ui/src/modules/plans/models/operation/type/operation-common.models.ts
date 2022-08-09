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

import { omit } from 'lodash-es'
import { OperationDetails } from 'spline-api'
import { SdWidgetSchema } from 'spline-common/data-view'

import { EventOperationProperty } from '../operation-property.models'


export const OPERATION_DEFAULT_PROPS: ReadonlyArray<string> = ['name']

export type OperationDetailsMainSectionResolver
    = (operationDetails: OperationDetails, primitiveProps: EventOperationProperty.ExtraPropertyValuePrimitive[]) => SdWidgetSchema[]

export function getBaseOperationDetailsSchema(operationDetails: OperationDetails,
                                              mainSectionResolver: OperationDetailsMainSectionResolver = () => [],
                                              extraDefaultProps: string[] = []): SdWidgetSchema[] {

    const properties = operationDetails.operation.properties
    const extraPropsNative = omit(properties, [...OPERATION_DEFAULT_PROPS, ...extraDefaultProps])
    const extraProps = EventOperationProperty.parseExtraOptions(extraPropsNative)

    const mainSection = mainSectionResolver(operationDetails, extraProps.primitive)

    const schema = [
        ...mainSection,
        // EXTRA PROPS :: JSON & EXPRESSION
        ...EventOperationProperty.jsonPropsToDvs(extraProps.json),
        ...EventOperationProperty.expressionPropsToDvs(extraProps.expression, operationDetails.schemasCollection),
    ]

    return schema.length ? schema : null
}
