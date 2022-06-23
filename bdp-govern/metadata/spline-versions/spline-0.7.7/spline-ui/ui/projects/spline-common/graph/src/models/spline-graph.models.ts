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


import { DagreSettings, Edge, Node, Orientation } from '@swimlane/ngx-graph'
import { omit } from 'lodash-es'
import { DynamicValueProvider } from 'spline-utils'


export type SgNodeSchema<TData extends object = {}, TOptions extends object = {}, TMetadata extends object = {}> = {
    id: string
    type?: string
    disallowSelection?: boolean // true by default
    data?: DynamicValueProvider<TData>
    options?: DynamicValueProvider<TOptions>
    metadata?: TMetadata
}

export type SgNodeNativeOptions = Omit<Node, 'id'>

export type SgNode<TData extends object = {}, TOptions extends object = {}> =
    & SgNodeSchema<TData, TOptions>
    &
    {
        nativeOptions?: SgNodeNativeOptions
    }

export type SgNativeNode<TData extends object = {}, TOptions extends object = {}> =
    & Node
    &
    {
        schema: SgNodeSchema<TData, TOptions>
    }

export type SgNodeLink = Edge

export type SgData = {
    nodes: SgNode<any>[]
    links: SgNodeLink[]
}

export function toSgNativeNode(node: SgNode): SgNativeNode {
    return {
        id: node.id,
        label: '', // it is needed for graph rendering
        ...(node?.nativeOptions ?? {}),
        schema: omit(node, ['nativeOptions']),
    }
}

export type SgLayoutSettings = DagreSettings

export const SG_DEFAULT_LAYOUT_SETTINGS: Readonly<SgLayoutSettings> = Object.freeze<SgLayoutSettings>({
    orientation: Orientation.TOP_TO_BOTTOM,
    marginX: 150,
    marginY: 64
} as SgLayoutSettings)


export function getNodeDomSelector(nodeId: string): string {
    return `[id="${nodeId}"]`
}

export function getLinkDomSelector(linkId: string): string {
    return `#${linkId}.link-group`
}

export function getNodeElementRef(rootElm: HTMLElement, nodeId: string): HTMLElement {
    const selector = getNodeDomSelector(nodeId)
    return rootElm.querySelector(selector)
}

export function switchGraphOrientation(orientation: Orientation): Orientation {
    return orientation === Orientation.LEFT_TO_RIGHT
        ? Orientation.TOP_TO_BOTTOM
        : Orientation.LEFT_TO_RIGHT
}
