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

import { AttributeLineage, OperationAttributeLineageType } from 'spline-api'
import { SgData } from 'spline-common/graph'
import { SplineAttributesTree } from 'spline-shared/attributes'

import { AttributeNodeControl } from '../../../models'


export namespace AttributeLineageDialog {

    export type Data = {
        graphData: SgData
        attributeTreeSchema: SplineAttributesTree.Tree
        lineageType: OperationAttributeLineageType
    }

    export function toData(attributeLineage: AttributeLineage,
                           attributeTreeSchema: SplineAttributesTree.Tree,
                           lineageType: OperationAttributeLineageType): Data {
        return {
            graphData: {
                links: attributeLineage.lineage.links,
                nodes: attributeLineage.lineage.nodes
                    .map(
                        node => ({
                            ...AttributeNodeControl.toSgNode(node),
                            disallowSelection: true
                        })
                    )
            },
            attributeTreeSchema,
            lineageType
        }
    }

}
