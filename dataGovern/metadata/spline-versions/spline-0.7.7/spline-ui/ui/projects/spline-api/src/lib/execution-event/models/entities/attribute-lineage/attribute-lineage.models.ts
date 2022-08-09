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

import { AttrSchemasCollection } from '../attribute'
import { Lineage, LineageNodeLink } from '../lineage'
import {
    OperationAttributeLineage,
    OperationAttributeLineageNode,
    OperationAttributeLineageType
} from '../operation-attribute-lineage'

import { AttributeLineageNode, AttributeLineageNodeType } from './attribute-linieage-node.models'


export type AttributeLineage = {
    lineage: Lineage<AttributeLineageNode>
}

export function toAttributeLineage(entity: OperationAttributeLineage,
                                   attrSchemaCollection: AttrSchemasCollection,
                                   executionPlanId: string,
                                   lineageType: OperationAttributeLineageType = OperationAttributeLineageType.Lineage): AttributeLineage {
    const refLineageSource = lineageType === OperationAttributeLineageType.Lineage
        ? entity.lineage
        : entity.impact

    return {
        lineage: {
            // we need to revert graph direction as BE returns it in the a wrong way
            // TODO: remove manual graph inversion after BE fixed.
            links: invertLinks(refLineageSource.links),
            nodes: refLineageSource.nodes.map(node => toAttributeLineageNode(node, attrSchemaCollection, executionPlanId))
        },
    }
}

// invert graph direction
export function invertLinks(links: LineageNodeLink[]): LineageNodeLink[] {
    return links.map(
        item => ({
            source: item.target,
            target: item.source
        })
    )
}

export function toAttributeLineageNode(node: OperationAttributeLineageNode,
                                       attrSchemaCollection: AttrSchemasCollection,
                                       executionPlanId: string): AttributeLineageNode {
    return {
        id: node.id,
        name: attrSchemaCollection[node.id].name,
        type: AttributeLineageNodeType.Attribute,
        operationId: node.originOpId,
        executionPlanId
    }
}

