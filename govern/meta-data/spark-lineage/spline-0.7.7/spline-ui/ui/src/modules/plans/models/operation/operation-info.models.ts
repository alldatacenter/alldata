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

import { Observable } from 'rxjs'
import { ExecutionPlanAgentInfo, Operation, OperationDetails, OperationType } from 'spline-api'
import { SdWidgetCard, SplineDataViewSchema } from 'spline-common/data-view'
import { SgNodeCardDataView } from 'spline-shared/data-view'
import { SgNodeControl } from 'spline-shared/graph'

import { attributesSchemaToDataViewSchema } from '../attribute'

import {
    OperationAggregate,
    OperationAlias,
    OperationFilter,
    OperationGeneric,
    OperationJoin,
    OperationProjection,
    OperationRead,
    OperationSort,
    OperationWrite,
} from './type'


export namespace OperationInfo {

    import NodeStyles = SgNodeControl.NodeStyles


    export type State = {
        operationDvs: SplineDataViewSchema
        inputsDvs: SplineDataViewSchema | null
        outputDvs: SplineDataViewSchema | null
        detailsDvs: SplineDataViewSchema | null
        inputsNumber: number
    }

    export function extractLabel(operation: Operation): string {
        return operation.type
    }

    export function toNodeType(operationType: OperationType | string, name: string): SgNodeControl.NodeType {

        const typeAsAName = Object.values(SgNodeControl.NodeType)

        switch (operationType) {
            case OperationType.Transformation: {
                return typeAsAName.includes(name as SgNodeControl.NodeType)
                    ? name as SgNodeControl.NodeType
                    : SgNodeControl.NodeType.Transformation
            }
            case OperationType.Read:
                return SgNodeControl.NodeType.Read
            case OperationType.Write:
                return SgNodeControl.NodeType.Write
            default:
                return SgNodeControl.NodeType.Generic
        }
    }

    export function getNodeStyles(operationType: OperationType | string, name: string): NodeStyles {
        const nodeType = toNodeType(operationType, name)
        return SgNodeControl.getNodeStyles(nodeType)
    }

    export function toDataViewSchema(operation: Operation): SplineDataViewSchema {
        const nodeStyles = getNodeStyles(operation.type, operation.name)

        return [
            SdWidgetCard.toSchema(
                {
                    color: nodeStyles.color,
                    icon: nodeStyles.icon,
                    title: operation.name,
                    iconTooltip: extractLabel(operation),
                    actions: [
                        SgNodeCardDataView.getNodeFocusAction(operation.id)
                    ]
                },
            ),
        ]
    }

    export function toInputsDvs(operationDetails: OperationDetails,
                                selectedAttributeId$: Observable<string | null>): SplineDataViewSchema | null {

        if (!operationDetails.inputs || !operationDetails.inputs?.length) {
            return null
        }

        return operationDetails.inputs
            .map(
                index => attributesSchemaToDataViewSchema(
                    operationDetails.schemas[index], operationDetails.dataTypes, selectedAttributeId$,
                ),
            )
    }

    export function toOutputsDvs(operationDetails: OperationDetails,
                                 selectedAttributeId$: Observable<string | null>): SplineDataViewSchema | null {

        if (operationDetails.output === null) {
            return null
        }

        return attributesSchemaToDataViewSchema(
            operationDetails.schemas[operationDetails.output], operationDetails.dataTypes, selectedAttributeId$,
        )
    }

    export function toDetailsDvs(
        operationDetails: OperationDetails,
        agentInfo: ExecutionPlanAgentInfo | undefined
    ): SplineDataViewSchema | null {
        const nodeType = toNodeType(operationDetails.operation.type, operationDetails.operation.name)
        if (agentInfo?.name == 'spline') {
            switch (nodeType) {
                case SgNodeControl.NodeType.Read:
                    return OperationRead.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Write:
                    return OperationWrite.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Filter:
                    return OperationFilter.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Alias:
                    return OperationAlias.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Join:
                    return OperationJoin.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Projection:
                    return OperationProjection.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Aggregate:
                    return OperationAggregate.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Sort:
                    return OperationSort.toDataViewSchema(operationDetails)

                case SgNodeControl.NodeType.Transformation:
                case SgNodeControl.NodeType.Generic:
                    return OperationGeneric.toDataViewSchema(operationDetails)

                default:
                    return null
            }
        }
        else {
            // no detailed view for metadata collected by 3rd-party agents
            return OperationGeneric.toDataViewSchema(operationDetails)
        }
    }

}
