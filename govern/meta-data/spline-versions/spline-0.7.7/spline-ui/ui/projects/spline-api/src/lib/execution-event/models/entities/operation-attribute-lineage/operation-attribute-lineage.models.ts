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

import { Lineage, LineageDto } from '../lineage'

import {
    OperationAttributeLineageNode,
    OperationAttributeLineageNodeDto,
    toOperationAttributeLineageNode
} from './operation-attribute-linieage-node.models'


export type OperationAttributeLineage = {
    impact: Lineage<OperationAttributeLineageNode>
    lineage: Lineage<OperationAttributeLineageNode>
}


export type OperationAttributeLineageDto = {
    impact: LineageDto<OperationAttributeLineageNodeDto>
    lineage: LineageDto<OperationAttributeLineageNodeDto>
}

export function toOperationAttributeLineage(entity: OperationAttributeLineageDto): OperationAttributeLineage {
    return {
        impact: {
            links: entity.impact.edges,
            nodes: entity.impact.nodes.map(toOperationAttributeLineageNode)
        },
        lineage: {
            links: entity.lineage.edges,
            nodes: entity.lineage.nodes.map(toOperationAttributeLineageNode)
        }
    }
}

