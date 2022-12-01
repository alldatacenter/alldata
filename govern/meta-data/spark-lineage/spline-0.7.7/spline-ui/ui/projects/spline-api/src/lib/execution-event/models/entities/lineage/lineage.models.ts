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

import { LineageNodeLink, LineageNodeLinkDto } from './lineage-node-link.models'
import { LineageNode } from './lineage-node.models'


export type Lineage<TNode extends LineageNode> = {
    links: LineageNodeLink[]
    nodes: TNode[]
}

export type LineageDto<TNode> = {
    edges: LineageNodeLinkDto[]
    nodes: TNode[]
}
