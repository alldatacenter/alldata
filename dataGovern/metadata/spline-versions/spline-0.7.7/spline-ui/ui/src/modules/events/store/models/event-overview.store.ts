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

import { differenceWith } from 'lodash-es'
import {
    ExecutionEventLineageNode,
    ExecutionEventLineageNodeType,
    ExecutionEventLineageOverview,
    ExecutionEventLineageOverviewDepth,
    Lineage,
    LineageNodeLink
} from 'spline-api'
import { SplineDataViewSchema } from 'spline-common/data-view'
import { SgData } from 'spline-common/graph'
import { SgNodeControl } from 'spline-shared/graph'
import { ProcessingStore, SplineEntityStore, StringHelpers } from 'spline-utils'

import { EventInfo, EventNodeControl, EventNodeInfo } from '../../models'


export namespace EventOverviewStore {

    export type State = {
        nodes: SplineEntityStore.EntityState<ExecutionEventLineageNode>
        links: LineageNodeLink[]
        executionEventId: string | null
        eventInfo: EventInfo | null
        loadingProcessing: ProcessingStore.EventProcessingState
        graphLoadingProcessing: ProcessingStore.EventProcessingState
        selectedNodeId: string | null
        targetNodeId: string | null
        targetExecutionPlanNodeId: string | null
        lineageDepth: ExecutionEventLineageOverviewDepth
        graphHasMoreDepth: boolean
        selectedNodeRelations: EventNodeInfo.NodeRelationsInfo | null
        targetNodeDvs: SplineDataViewSchema | null
        targetExecutionPlanNodeDvs: SplineDataViewSchema | null
        graphNodeView: SgNodeControl.NodeView
        graphData: SgData | null
    }


    export const GRAPH_DEFAULT_DEPTH = 2

    const DEFAULT_LINEAGE_DEPTH = Object.freeze<ExecutionEventLineageOverviewDepth>({
        depthComputed: GRAPH_DEFAULT_DEPTH,
        depthRequested: GRAPH_DEFAULT_DEPTH,
    })

    export function getDefaultState(): State {
        return {
            nodes: SplineEntityStore.getDefaultState<ExecutionEventLineageNode>(),
            links: [],
            executionEventId: null,
            eventInfo: null,
            loadingProcessing: ProcessingStore.getDefaultProcessingState(),
            graphLoadingProcessing: ProcessingStore.getDefaultProcessingState(),
            selectedNodeId: null,
            targetNodeId: null,
            targetNodeDvs: null,
            selectedNodeRelations: null,
            targetExecutionPlanNodeId: null,
            targetExecutionPlanNodeDvs: null,
            lineageDepth: { ...DEFAULT_LINEAGE_DEPTH },
            graphHasMoreDepth: calculateHasMoreDepth(DEFAULT_LINEAGE_DEPTH),
            graphNodeView: SgNodeControl.NodeView.Detailed,
            graphData: null,
        }
    }

    export function reduceLineageOverviewData(state: State,
                                              executionEventId: string,
                                              lineageOverview: ExecutionEventLineageOverview): State {

        const targetEdge = lineageOverview.lineage.links
            .find(
                x => x.target === lineageOverview.executionEventInfo.targetDataSourceId,
            )
        const eventNode = targetEdge
            ? lineageOverview.lineage.nodes.find(x => x.id === targetEdge.source)
            : undefined

        const nodesState = SplineEntityStore.addAll(lineageOverview.lineage.nodes, state.nodes)

        const newState = {
            ...state,
            nodes: nodesState,
            links: lineageOverview.lineage.links,
            eventInfo: {
                id: executionEventId,
                name: eventNode ? eventNode.name : 'NaN',
                applicationId: lineageOverview.executionEventInfo.applicationId,
                executedAt: new Date(lineageOverview.executionEventInfo.timestamp),
                executionPlan: eventNode
                    ? {
                        id: eventNode.id,
                        name: eventNode.name
                    }
                    : undefined
            },
            lineageDepth: lineageOverview.executionEventInfo.lineageDepth,
            graphHasMoreDepth: calculateHasMoreDepth(lineageOverview.executionEventInfo.lineageDepth),
            targetNodeId: lineageOverview.executionEventInfo.targetDataSourceId,
            targetNodeDvs: EventNodeInfo.nodeToDataSchema(
                SplineEntityStore.selectOne(lineageOverview.executionEventInfo.targetDataSourceId, nodesState)
            ),
            targetExecutionPlanNodeId: eventNode.id,
        }

        return calculateTargetExecutionPlanNodeDvs(calculateGraphDataMiddleware(newState))
    }

    export function calculateTargetExecutionPlanNodeDvs(state: State): State {
        const targetPlanNode: ExecutionEventLineageNode = SplineEntityStore.selectOne(state.targetExecutionPlanNodeId, state.nodes)
        return {
            ...state,
            targetExecutionPlanNodeDvs: EventNodeInfo.nodeToDataSchema(
                targetPlanNode, selectNodeRelations(state, targetPlanNode)
            ),
        }
    }

    export function __reduceFakeHistoryNode(state: State, nodeId: string): State {
        const fakeNodeJob: ExecutionEventLineageNode = {
            id: `__fakeJob${StringHelpers.guid()}`,
            name: 'Some Fake Job',
            type: ExecutionEventLineageNodeType.Execution
        }

        const fakeNodeDs: ExecutionEventLineageNode = {
            id: `__fakeDataSource${StringHelpers.guid()}`,
            name: 'Some Fake Data Source',
            type: ExecutionEventLineageNodeType.DataSource
        }

        const newState = {
            ...state,
            nodes: SplineEntityStore.addMany([fakeNodeJob, fakeNodeDs], state.nodes),
            links: [
                {
                    source: fakeNodeDs.id,
                    target: fakeNodeJob.id
                },
                {
                    source: fakeNodeJob.id,
                    target: nodeId
                },
                ...state.links
            ],
        }
        return calculateGraphDataMiddleware(newState)
    }

    export function __reduceFakeFutureNode(state: State, nodeId: string): State {
        const fakeNodeJob: ExecutionEventLineageNode = {
            id: `__fakeJob${StringHelpers.guid()}`,
            name: 'Some Fake Job',
            type: ExecutionEventLineageNodeType.Execution
        }

        const fakeNodeDs: ExecutionEventLineageNode = {
            id: `__fakeDataSource${StringHelpers.guid()}`,
            name: 'Some Fake Data Source',
            type: ExecutionEventLineageNodeType.DataSource
        }

        const newState = {
            ...state,
            nodes: SplineEntityStore.addMany([fakeNodeJob, fakeNodeDs], state.nodes),
            links: [
                {
                    target: fakeNodeDs.id,
                    source: fakeNodeJob.id
                },
                {
                    target: fakeNodeJob.id,
                    source: nodeId
                },
                ...state.links
            ]
        }
        return calculateGraphDataMiddleware(newState)
    }

    export function reduceGraphNodeView(state: State, graphNodeView: SgNodeControl.NodeView): State {
        return calculateGraphDataMiddleware({
            ...state,
            graphNodeView
        })
    }

    export function calculateGraphDataMiddleware(state: State): State {
        return {
            ...state,
            graphData: calculateGraphData(state)
        }
    }

    export function calculateGraphData(state: State): SgData {
        const nodesList = EventOverviewStore.selectAllNodes(state)

        const graphData = {
            links: state.links,
            nodes: nodesList
                // map node source data to the SgNode schema
                .map(
                    nodeSource => EventNodeControl.toSgNode(nodeSource, state.graphNodeView),
                ),
        }

        return graphData

        // TODO: implement that logic after BE related part will be ready
        // const lineageData = {
        //     nodes: nodesList,
        //     links: state.links
        // }
        //
        // const loadHistoryGraph = getLoadHistoryGraphData(lineageData)
        // const loadFutureGraph = getLoadFutureGraphData(lineageData)
        //
        // return {
        //     links: [...loadHistoryGraph.links, ...graphData.links, ...loadFutureGraph.links],
        //     nodes: [...loadHistoryGraph.nodes, ...graphData.nodes, ...loadFutureGraph.nodes],
        // }
    }

    export function reduceSelectedNodeId(nodeId: string | null, state: State): State {
        const selectedNode = nodeId ? selectNode(state, nodeId) : null

        return {
            ...state,
            selectedNodeId: nodeId,
            selectedNodeRelations: selectedNode
                ? selectNodeRelations(state, selectedNode)
                : null
        }
    }

    export function selectNodeRelations(state: State, node: ExecutionEventLineageNode): EventNodeInfo.NodeRelationsInfo {
        return {
            node: node,
            children: selectChildrenNodes(state, node.id),
            parents: selectParentNodes(state, node.id),
        }
    }

    export function selectAllNodes(state: State): ExecutionEventLineageNode[] {
        return SplineEntityStore.selectAll(state.nodes)
    }

    export function selectNode(state: State, nodeId: string): ExecutionEventLineageNode | undefined {
        return SplineEntityStore.selectOne(nodeId, state.nodes)
    }

    export function selectChildrenNodes(state: State, nodeId: string): ExecutionEventLineageNode[] {
        return state.links
            .filter(link => link.source === nodeId)
            .map(link => SplineEntityStore.selectOne(link.target, state.nodes))
    }

    export function selectParentNodes(state: State, nodeId: string): ExecutionEventLineageNode[] {
        return state.links
            .filter(link => link.target === nodeId)
            .map(link => SplineEntityStore.selectOne(link.source, state.nodes))
    }

    function calculateHasMoreDepth(lineageDepth: ExecutionEventLineageOverviewDepth): boolean {
        return lineageDepth.depthRequested === lineageDepth.depthComputed
    }

    export function getLoadHistoryGraphData(lineageData: Lineage<ExecutionEventLineageNode>): SgData {
        // select DS nodes with no parents
        const dsNodesIds = lineageData.nodes
            .filter(item => item.type === ExecutionEventLineageNodeType.DataSource)
            .map(item => item.id)

        const dsNodesIdsWithParent = lineageData.links
            .filter(item => dsNodesIds.includes(item.target))
            .map(item => item.target)

        const dsNodesIdsNoParent = differenceWith(dsNodesIds, dsNodesIdsWithParent)

        // add new add button to the node with no parent

        return {
            links: dsNodesIdsNoParent.map(
                nodeId => ({
                    source: EventNodeControl.toLoadMoreNodeId(nodeId),
                    target: nodeId
                })
            ),
            nodes: dsNodesIdsNoParent.map(
                nodeId => EventNodeControl.toLoadHistorySgNode(nodeId)
            )
        }
    }

    export function getLoadFutureGraphData(lineageData: Lineage<ExecutionEventLineageNode>): SgData {
        // select DS nodes with no parents
        const dsNodesIds = lineageData.nodes
            .filter(item => item.type === ExecutionEventLineageNodeType.DataSource)
            .map(item => item.id)

        const dsNodesIdsWithChildren = lineageData.links
            .filter(item => dsNodesIds.includes(item.source))
            .map(item => item.source)

        const dsNodesIdsNoChildren = differenceWith(dsNodesIds, dsNodesIdsWithChildren)

        // add new add button to the node with no parent

        return {
            links: dsNodesIdsNoChildren.map(
                nodeId => ({
                    target: EventNodeControl.toLoadMoreNodeId(nodeId),
                    source: nodeId
                })
            ),
            nodes: dsNodesIdsNoChildren.map(
                nodeId => EventNodeControl.toLoadFutureSgNode(nodeId)
            )
        }
    }



}
