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

import { dataSourceUriToName, ExecutionEventLineageNode, ExecutionEventLineageNodeType } from 'spline-api'
import { SgNode, SgNodeCircle, SgNodeCircleButton, SgNodeDefault } from 'spline-common/graph'
import { SgNodeControl } from 'spline-shared/graph'
import NodeType = SgNodeControl.NodeType
import NodeView = SgNodeControl.NodeView


export namespace EventNodeControl {

    import NodeStyles = SgNodeControl.NodeStyles


    const LOAD_MORE_NODE_ID_PREFIX = '__loadMore__'

    export enum NodeControlEvent {
        LaunchExecutionEvent = 'LaunchExecutionEvent',
        LoadHistory = 'LoadHistory',
        LoadFuture = 'LoadFuture'
    }

    export function extractNodeName(nodeSource: ExecutionEventLineageNode): string {
        return dataSourceUriToName(nodeSource.name)
    }

    export function getNodeStyles(nodeSource: ExecutionEventLineageNode): NodeStyles {
        switch (nodeSource.type) {
            case ExecutionEventLineageNodeType.DataSource:
                return SgNodeControl.getNodeStyles(NodeType.DataSource)
            case ExecutionEventLineageNodeType.Execution:
                return SgNodeControl.getNodeStyles(NodeType.ExecutionPlan)
            default:
                throw new Error(`Unknown node type ${nodeSource.type}`)
        }
    }

    export function toSgNode(nodeSource: ExecutionEventLineageNode,
                             nodeView: NodeView = NodeView.Detailed): SgNode {

        const nodeStyles = getNodeStyles(nodeSource)

        const defaultActions = [
            ...SgNodeControl.getNodeRelationsHighlightActions(),
        ]

        switch (nodeView) {
            case SgNodeControl.NodeView.Compact:
                return SgNodeCircle.toNode(
                    nodeSource.id,
                    {
                        tooltip: extractNodeName(nodeSource),
                        ...nodeStyles,
                    },
                )
            case SgNodeControl.NodeView.Detailed:
            default:
                return SgNodeDefault.toNode(
                    nodeSource.id,
                    {
                        label: extractNodeName(nodeSource),
                        ...nodeStyles,
                        inlineActions: nodeSource.type === ExecutionEventLineageNodeType.DataSource
                            ? [
                                ...defaultActions
                            ]
                            : [
                                {
                                    icon: 'launch',
                                    tooltip: 'EVENTS.EVENT_NODE_CONTROL__ACTION__LAUNCH',
                                    event: {
                                        type: NodeControlEvent.LaunchExecutionEvent
                                    }
                                },
                                ...defaultActions
                            ],
                    },
                )
        }
    }

    export function toLoadHistorySgNode(nodeId: string): SgNode {
        return SgNodeCircleButton.toNode(
            toLoadMoreNodeId(nodeId),
            {
                eventName: NodeControlEvent.LoadHistory,
                icon: 'history',
                tooltip: 'Load More',
            },
        )
    }

    export function toLoadMoreNodeId(nodeId: string): string {
        return `${LOAD_MORE_NODE_ID_PREFIX}${nodeId}`
    }

    export function loadMoreNodeToNativeNodeId(loadMoreNodeId: string): string {
        return loadMoreNodeId.replace(LOAD_MORE_NODE_ID_PREFIX, '')
    }

    export function toLoadFutureSgNode(nodeId: string): SgNode {
        return SgNodeCircleButton.toNode(
            toLoadMoreNodeId(nodeId),
            {
                eventName: NodeControlEvent.LoadFuture,
                icon: 'more_time',
                tooltip: 'Load More',
            },
        )
    }
}

