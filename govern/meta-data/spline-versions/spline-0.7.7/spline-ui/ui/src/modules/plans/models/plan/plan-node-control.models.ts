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

import { ExecutionPlanLineageNode } from 'spline-api'
import { SgNode, SgNodeCircle, SgNodeDefault } from 'spline-common/graph'
import { SgNodeControl } from 'spline-shared/graph'

import { OperationInfo } from '../operation'


export namespace PlanNodeControl {

    import NodeView = SgNodeControl.NodeView


    export function extractNodeName(nodeSource: ExecutionPlanLineageNode): string {
        return nodeSource.name
    }

    export function toSgNode(nodeSource: ExecutionPlanLineageNode,
                             nodeView: NodeView = NodeView.Detailed): SgNode {
        const nodeStyles = OperationInfo.getNodeStyles(nodeSource.type, nodeSource.name)

        const defaultActions = [
            ...SgNodeControl.getNodeRelationsHighlightActions(),
        ]

        switch (nodeView) {
            case NodeView.Compact:
                return SgNodeCircle.toNode(
                    nodeSource.id,
                    {
                        tooltip: extractNodeName(nodeSource),
                        ...nodeStyles,
                    },
                )
            case NodeView.Detailed:
            default:
                return SgNodeDefault.toNode(
                    nodeSource.id,
                    {
                        label: extractNodeName(nodeSource),
                        ...nodeStyles,
                        inlineActions: defaultActions,
                    },
                )
        }
    }
}

