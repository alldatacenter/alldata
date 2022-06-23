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


import { SplineColors } from 'spline-common'
import { SgNodeDefault } from 'spline-common/graph'


export namespace SgNodeControl {

    export type NodeStyles = {
        icon: string
        color: string
    }

    export enum NodeType {
        DataSource = 'DataSource',
        ExecutionPlan = 'ExecutionPlan',
        Read = 'Read',
        Write = 'Write',
        Join = 'Join',
        Filter = 'Filter',
        Transformation = 'Transformation',
        Alias = 'SubqueryAlias',
        Generic = 'Generic',
        Projection = 'Project',
        Aggregate = 'Aggregate',
        Sort = 'Sort',
        Attribute = 'Attribute',
    }

    export type NodeView = 'Detailed' | 'Compact'
    export const NodeView = {
        Detailed: 'Detailed' as NodeView,
        Compact: 'Compact' as NodeView,
    }

    export const DEFAULT_NODE_STYLES: NodeStyles = Object.freeze<NodeStyles>({
        icon: 'extension',
        color: SplineColors.GREY,
    })

    export const NODE_STYLES_MAP: ReadonlyMap<NodeType, NodeStyles> =
        new Map<NodeType, NodeStyles>([
            [
                NodeType.DataSource,
                {
                    icon: 'description',
                    color: SplineColors.BLUE,
                },
            ],
            [
                NodeType.ExecutionPlan,
                {
                    icon: 'cog-transfer-outline',
                    color: SplineColors.ORANGE,
                },
            ],
            [
                NodeType.Read,
                {
                    icon: 'database',
                    color: SplineColors.ORANGE,
                },
            ],
            [
                NodeType.Write,
                {
                    icon: 'save',
                    color: SplineColors.BLACK,
                },
            ],
            [
                NodeType.Join,
                {
                    icon: 'source-branch',
                    color: SplineColors.ORANGE,
                },
            ],
            [
                NodeType.Transformation,
                {
                    icon: 'shuffle-variant',
                    color: SplineColors.BLUE,
                },
            ],
            [
                NodeType.Projection,
                {
                    icon: 'shuffle-variant',
                    color: SplineColors.BLUE,
                },
            ],
            [
                NodeType.Filter,
                {
                    icon: 'filter_alt',
                    color: SplineColors.SMILE,
                },
            ],
            [
                NodeType.Alias,
                {
                    icon: 'local_offer',
                    color: SplineColors.BLUE,
                },
            ],
            [
                NodeType.Generic,
                {
                    icon: 'alpha-o-circle-outline',
                    color: SplineColors.BLUE,
                },
            ],
            [
                NodeType.Aggregate,
                {
                    icon: 'arrow-collapse-all',
                    color: SplineColors.GREEN,
                },
            ],
            [
                NodeType.Sort,
                {
                    icon: 'sort_by_alpha',
                    color: SplineColors.GREEN,
                },
            ],
            [
                NodeType.Attribute,
                {
                    icon: 'local_offer',
                    color: SplineColors.ORANGE,
                },
            ]
        ])

    export function getNodeStyles(type: NodeType): NodeStyles {
        return NODE_STYLES_MAP.get(type) ?? { ...DEFAULT_NODE_STYLES }
    }

    export enum NodeControlEvent {
        HighlightRelations = 'HighlightRelations',
        Focus = 'Focus',
    }

    export function getNodeRelationsHighlightToggleActions(): SgNodeDefault.InlineAction {
        return {
            icon: 'eye-outline',
            tooltip: 'SHARED.SG_NODE_CONTROL__ACTION__HIGHLIGHT_RELATIONS__TOGGLE',
            event: {
                type: NodeControlEvent.HighlightRelations,
            }
        }
    }

    export function getNodeRelationsHighlightActions(): SgNodeDefault.InlineAction[] {
        return [
            getNodeRelationsHighlightToggleActions(),
        ]
    }

    export function getNodeFocusAction(): SgNodeDefault.InlineAction {
        return {
            icon: 'target',
            tooltip: 'SHARED.SG_NODE_CONTROL__ACTION__FOCUS',
            event: {
                type: NodeControlEvent.Focus
            }
        }
    }

}

