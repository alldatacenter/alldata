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

import {
    dataSourceUriToName,
    ExecutionEventLineageNode,
    ExecutionEventLineageNodeType,
    OperationDetails,
    operationIdToExecutionPlanId,
    uriToDatsSourceId
} from 'spline-api'
import { SplineCardHeader } from 'spline-common'
import { SdWidgetCard, SdWidgetRecordsList, SdWidgetSchema, SdWidgetSimpleRecord, SplineDataViewSchema } from 'spline-common/data-view'
import { SdWidgetAttributesTree } from 'spline-shared/attributes'
import { SgEventNodeInfoShared, SgNodeCardDataView } from 'spline-shared/data-view'
import { ProcessingStore } from 'spline-utils'

import { EventNodeControl } from './event-node-control.models'


export namespace EventNodeInfo {

    export enum WidgetEvent {
        LaunchExecutionEvent = 'LaunchExecutionEvent'
    }

    export function getNodeInfoTooltip(nodeSource: ExecutionEventLineageNode): string {
        return nodeSource.type === ExecutionEventLineageNodeType.DataSource
            ? 'EVENTS.EVENT_NODE_INFO__TOOLTIP__DATA_SOURCE'
            : 'EVENTS.EVENT_NODE_INFO__TOOLTIP__EXECUTION_PLAN'
    }

    function getNodeDefaultActions(nodeId: string): SplineCardHeader.Action[] {
        return [
            SgNodeCardDataView.getNodeRelationsHighlightToggleAction(nodeId),
            SgNodeCardDataView.getNodeFocusAction(nodeId),
        ]
    }

    export function nodeToDataSchema(
        nodeSource: ExecutionEventLineageNode,
        nodeRelations?: EventNodeInfo.NodeRelationsInfo): SdWidgetSchema {

        return nodeSource.type === ExecutionEventLineageNodeType.DataSource
            ? dataSourceNodeToDataSchema(nodeSource)
            : executionNodeToDataSchema(nodeSource, nodeRelations)
    }

    export function executionNodeToDataSchema(
        nodeSource: ExecutionEventLineageNode,
        nodeRelations: EventNodeInfo.NodeRelationsInfo): SdWidgetSchema {

        const nodeStyles = EventNodeControl.getNodeStyles(nodeSource)

        const actions: SplineCardHeader.Action[] = [
            ...getNodeDefaultActions(nodeSource.id),
            {
                icon: 'launch',
                tooltip: 'EVENTS.EVENT_NODE_CONTROL__ACTION__LAUNCH',
                event: SgNodeCardDataView.toNodeWidgetEventInfo(WidgetEvent.LaunchExecutionEvent, nodeSource.id)
            },
        ]

        return SdWidgetCard.toSchema(
            {
                color: nodeStyles.color,
                icon: nodeStyles.icon,
                title: EventNodeControl.extractNodeName(nodeSource),
                iconTooltip: getNodeInfoTooltip(nodeSource),
                actions,
            },
            [
                ...(
                    nodeSource?.systemInfo
                        ? [SdWidgetSimpleRecord.toSchema({
                            label: 'EVENTS.EVENT_NODE_INFO__SYSTEM_INFO',
                            value: `${nodeSource.systemInfo.name} ${nodeSource.systemInfo.version}`,
                        })]
                        : []
                ),
                ...(
                    nodeSource?.agentInfo
                        ? [SdWidgetSimpleRecord.toSchema({
                            label: 'EVENTS.EVENT_NODE_INFO__AGENT_INFO',
                            value: `${nodeSource.agentInfo.name} ${nodeSource.agentInfo.version}`,
                        })]
                        : []
                ),
                ...(
                    nodeRelations?.children[0]?.name
                        ? [SdWidgetRecordsList.toSchema([
                            {
                                value: dataSourceUriToName(nodeRelations.children[0].name),
                                description: nodeRelations.children[0].name,
                                routerLink: ['/data-sources/overview', uriToDatsSourceId(nodeRelations.children[0].name)]
                            }
                        ],
                        'EVENTS.EVENT_NODE_INFO__OUTPUT_DATA_SOURCES',
                        )]
                        :
                        []
                ),
                ...(
                    nodeRelations?.parents?.length
                        ? [
                            SdWidgetRecordsList.toSchema(
                                nodeRelations?.parents
                                    .map(node => ({
                                        value: dataSourceUriToName(node.name),
                                        description: node.name,
                                        routerLink: ['/data-sources/overview', uriToDatsSourceId(node.name)]
                                    })),
                                'EVENTS.EVENT_NODE_INFO__INPUT_DATA_SOURCES',
                            )
                        ]
                        :
                        []
                ),
            ],
        )
    }

    export function dataSourceNodeToDataSchema(nodeSource: ExecutionEventLineageNode): SdWidgetSchema {
        const nodeStyles = EventNodeControl.getNodeStyles(nodeSource)
        return SdWidgetCard.toSchema(
            {
                color: nodeStyles.color,
                icon: nodeStyles.icon,
                title: EventNodeControl.extractNodeName(nodeSource),
                iconTooltip: getNodeInfoTooltip(nodeSource),
                actions: getNodeDefaultActions(nodeSource.id),
            },
            [
                SdWidgetSimpleRecord.toSchema({
                    label: 'Name',
                    value: dataSourceUriToName(nodeSource.name),
                    routerLink: ['/data-sources/overview', uriToDatsSourceId(nodeSource.name)]
                }),
                SdWidgetSimpleRecord.toSchema({
                    label: 'URI',
                    value: nodeSource.name,
                }),
            ],
        )
    }

    export type NodeRelationsInfo = {
        node: ExecutionEventLineageNode
        parents: ExecutionEventLineageNode[]
        children: ExecutionEventLineageNode[]
    }

    export type DataSourceSchemaDetails = {
        executionPlansIds: string[]
        dataViewSchema: SplineDataViewSchema
    }

    export type NodeInfoViewState = {
        nodeDvs: SplineDataViewSchema | null
        dataSourceSchemaDetailsList: DataSourceSchemaDetails[]
        loadingProcessing: ProcessingStore.EventProcessingState
    }

    export function getDefaultState(): NodeInfoViewState {
        return {
            nodeDvs: null,
            dataSourceSchemaDetailsList: [],
            loadingProcessing: ProcessingStore.getDefaultProcessingState(true)
        }
    }

    export function getOperationsDataSourceSchemasDvs(operationsDetailsList: OperationDetails[]): DataSourceSchemaDetails[] {

        const operationsDetailsTreeMap = SgEventNodeInfoShared.toOperationDetailsAttrTreeMap(operationsDetailsList)

        return Object.values(operationsDetailsTreeMap)
            .map(currentOperationsInfoList => {

                // as all schemas are the same we can take just the first operation as a reference to build schema.
                const treeData = currentOperationsInfoList[0].attrTree

                const executionPlansIds = currentOperationsInfoList
                    .map(
                        ({ operationInfo }) => operationIdToExecutionPlanId(operationInfo.operation.id)
                    )

                return {
                    executionPlansIds,
                    dataViewSchema: SdWidgetCard.toContentOnlySchema(
                        SdWidgetAttributesTree.toSchema(
                            treeData,
                            {
                                allowAttrSelection: false,
                            }
                        ),
                    )
                }
            })
    }

    export function reduceNodeRelationsState(state: NodeInfoViewState,
                                             nodeRelations: EventNodeInfo.NodeRelationsInfo,
                                             operationsInfoList: OperationDetails[] = []): NodeInfoViewState {
        return {
            ...state,
            nodeDvs: nodeToDataSchema(nodeRelations.node, nodeRelations),
            dataSourceSchemaDetailsList: operationsInfoList.length > 0 ? getOperationsDataSourceSchemasDvs(operationsInfoList) : [],
        }
    }

}
