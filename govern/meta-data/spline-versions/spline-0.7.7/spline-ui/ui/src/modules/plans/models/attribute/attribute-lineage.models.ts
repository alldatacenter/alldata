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

import { keyBy } from 'lodash-es'
import { OperationAttributeLineage, OperationAttributeLineageNode, OperationAttributeLineageType } from 'spline-api'
import { SplineColors } from 'spline-common'


export const LINEAGE_TYPE_COLOR_MAP: Readonly<Record<OperationAttributeLineageType, string>>
    = Object.freeze<Record<OperationAttributeLineageType, string>>({
        [OperationAttributeLineageType.Usage]: SplineColors.BLACK,
        [OperationAttributeLineageType.Lineage]: SplineColors.GREEN_LIGHT,
        [OperationAttributeLineageType.Impact]: SplineColors.SMILE,
    })

// TODO: write some tests
export function extractImpactRootAttributeNode(graph: OperationAttributeLineage): OperationAttributeLineageNode {
    const impactedAttrIds = keyBy(graph.impact.links, item => item.source)
    return graph.impact.nodes.find(node => !impactedAttrIds[node.id])
}

// TODO: write some tests
export function getLineageAttributesNodes(graph: OperationAttributeLineage): OperationAttributeLineageNode[] {
    const primaryAttribute = extractImpactRootAttributeNode(graph)
    return graph?.lineage?.nodes?.length
        ? graph.lineage.nodes.filter(node => node.id !== primaryAttribute.id)
        : []
}

// TODO: write some tests
export function getImpactAttributesNodes(graph: OperationAttributeLineage): OperationAttributeLineageNode[] {
    const primaryAttribute = extractImpactRootAttributeNode(graph)
    return graph?.impact?.nodes?.length
        ? graph.impact.nodes.filter(node => node.id !== primaryAttribute.id)
        : []
}

export type AttributeLineageNodesMap = Record<OperationAttributeLineageType, Set<string>>

// TODO: write some tests
export function extractAttributeLineageNodesMap(graph: OperationAttributeLineage | null): AttributeLineageNodesMap {

    if (graph === null) {
        return {
            [OperationAttributeLineageType.Usage]: new Set(),
            [OperationAttributeLineageType.Lineage]: new Set(),
            [OperationAttributeLineageType.Impact]: new Set(),
        }
    }

    const primaryAttribute = extractImpactRootAttributeNode(graph)
    const impactAttributes = getImpactAttributesNodes(graph)
    const lineageAttributes = getLineageAttributesNodes(graph)

    const primaryNodeIds = [primaryAttribute.originOpId, ...primaryAttribute.transOpIds]
    const impactNodesIds = impactAttributes
        .reduce((acc, current) => [
            ...acc,
            current.originOpId,
            ...current.transOpIds,
        ], [])

    const lineageNodesIds = lineageAttributes
        .reduce((acc, current) => [
            ...acc,
            current.originOpId,
            ...current.transOpIds,
        ], [])

    return {
        [OperationAttributeLineageType.Usage]: new Set(primaryNodeIds),
        [OperationAttributeLineageType.Lineage]: new Set(lineageNodesIds),
        [OperationAttributeLineageType.Impact]: new Set(impactNodesIds),
    }
}
