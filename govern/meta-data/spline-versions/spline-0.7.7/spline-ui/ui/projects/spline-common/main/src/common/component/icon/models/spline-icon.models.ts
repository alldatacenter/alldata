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

export namespace SplineIcon {

    export type Color = string
    export type Icon = typeof ICONS[number]

    export const ICONS = [
        'database',
        'cog-transfer',
        'arrow-down-circle',
        'source-branch',
        'head-cog',
        'code-json',
        'shuffle-variant',
        'arrow-collapse-all',
        'group',
        'code-brackets',
        'target',
        'eye-outline',
        'vector-line',
        'graph-outline',
        'chevron-double-left',
        'chevron-triple-left',
        'chevron-triple-right',
        'eye-off-outline',
        'ray-start-arrow',
        'ray-end-arrow',
        'arrow-expand-down',
        'arrow-expand-up',
        'arrow-split-horizontal',
        'swap-vertical-bold',
        'arrow-expand-vertical',
        'arrow-expand',
        'arrow-collapse',
        'parent-nodes',
        'children-nodes',
        'transit-connection',
        'cog-transfer-outline',
        'alpha-o-circle-outline',
    ]

    export const ICON_SET: Set<string> = new Set(ICONS)
}
