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

import { AttributeLineageNode } from 'spline-api'
import { SgNode, SgNodeDefault, SgNodeViewDefault } from 'spline-common/graph'
import { SgNodeControl } from 'spline-shared/graph'


export namespace AttributeNodeControl {

    export function toSgNode(nodeSource: AttributeLineageNode): SgNode {

        const nodeStyles = SgNodeControl.getNodeStyles(SgNodeControl.NodeType.Attribute)

        // TODO: do we really need these actions here?
        // const defaultActions = [
        //     ...SgNodeControl.getNodeRelationsHighlightActions(),
        // ]

        return SgNodeDefault.toNode(
            nodeSource.id,
            {
                label: nodeSource.name,
                ...nodeStyles,
                // inlineActions: defaultActions,
                actionsPosition: SgNodeViewDefault.ActionsPosition.right
            },
        )

    }
}
