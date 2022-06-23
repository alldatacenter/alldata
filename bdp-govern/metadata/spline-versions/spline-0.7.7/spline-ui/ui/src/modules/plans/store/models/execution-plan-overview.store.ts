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
    ExecutionPlan,
    ExecutionPlanLineageNode,
    ExecutionPlanLineageOverview,
    LineageNodeLink,
    OperationAttributeLineage
} from 'spline-api'
import { SgData } from 'spline-common/graph'
import { SgNodeControl } from 'spline-shared/graph'
import { ProcessingStore, SplineEntityStore } from 'spline-utils'

import { PlanNodeControl } from '../../models'


export namespace ExecutionPlanOverviewStore {

    export type State = {
        nodes: SplineEntityStore.EntityState<ExecutionPlanLineageNode>
        executionPlanId: string | null
        links: LineageNodeLink[]
        executionPlan: ExecutionPlan | null
        loading: ProcessingStore.EventProcessingState

        selectedNodeId: string | null
        selectedAttributeId: string | null

        attributeLineage: OperationAttributeLineage | null
        attributeLineageLoading: ProcessingStore.EventProcessingState

        graphNodeView: SgNodeControl.NodeView
        graphData: SgData | null
    }

    export function getDefaultState(): State {
        return {
            nodes: SplineEntityStore.getDefaultState<ExecutionPlanLineageNode>(),
            executionPlanId: null,
            links: [],
            executionPlan: null,
            loading: ProcessingStore.getDefaultProcessingState(),

            selectedNodeId: null,
            selectedAttributeId: null,

            attributeLineage: null,
            attributeLineageLoading: ProcessingStore.getDefaultProcessingState(),

            graphNodeView: SgNodeControl.NodeView.Detailed,
            graphData: null
        }
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
        const nodesList = selectAllNodes(state)
        return {
            links: state.links,
            nodes: nodesList
                // map node source data to the SgNode schema
                .map(
                    nodeSource => PlanNodeControl.toSgNode(nodeSource, state.graphNodeView),
                ),
        }
    }

    export function reduceLineageOverviewData(state: State,
                                              executionEventId: string,
                                              executionPlanOverview: ExecutionPlanLineageOverview): State {
        const newState = {
            ...state,
            nodes: SplineEntityStore.addAll(executionPlanOverview.lineage.nodes, state.nodes),
            links: executionPlanOverview.lineage.links,
            executionPlan: executionPlanOverview.executionPlan,
        }

        return calculateGraphDataMiddleware(newState)
    }

    export function selectAllNodes(state: State): ExecutionPlanLineageNode[] {
        return SplineEntityStore.selectAll(state.nodes)
    }

    export function selectNode(state: State, nodeId: string): ExecutionPlanLineageNode | undefined {
        return SplineEntityStore.selectOne(nodeId, state.nodes)
    }

}
