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

import { LineageNodeLink } from 'spline-api'


export namespace SgRelations {

    export type NodeHighlightFn = (highlightedRelations: string[], nodeId: string, links: LineageNodeLink[]) => string[]

    export function toggleSelection(highlightedRelations: string[], nodeId: string, links: LineageNodeLink[]): string[] {

        const relatedNodesIds = links
            .filter(link => nodeId === link.source || nodeId === link.target)
            .reduce((acc, currentLink) => [...acc, currentLink.source, currentLink.target], [])

        return toggleRelations(relatedNodesIds, highlightedRelations)

    }

    export function toggleChildrenSelection(highlightedRelations: string[], nodeId: string, links: LineageNodeLink[]): string[] {

        const relatedNodesIds = links
            .filter(link => nodeId === link.source)
            .reduce((acc, currentLink) => [...acc, currentLink.target], [nodeId])

        return toggleRelations(relatedNodesIds, highlightedRelations)

    }

    export function toggleParentsSelection(highlightedRelations: string[], nodeId: string, links: LineageNodeLink[]): string[] {

        const relatedNodesIds = links
            .filter(link => nodeId === link.target)
            .reduce((acc, currentLink) => [...acc, currentLink.source], [nodeId])

        return toggleRelations(relatedNodesIds, highlightedRelations)

    }

    function toggleRelations(relatedNodesIds: string[], highlightedRelations: string[]): string[] {
        const notHighlightedNodesIds = relatedNodesIds
            .filter(id => !highlightedRelations.includes(id))

        if (notHighlightedNodesIds.length === 0) {
            return highlightedRelations
                .filter(
                    id => !relatedNodesIds.includes(id),
                )
        }

        return [
            ...highlightedRelations,
            ...notHighlightedNodesIds,
        ]
    }

}
