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

import { ThemePalette } from '@angular/material/core'
import { NodeDimension } from '@swimlane/ngx-graph'

import { SgNode, SgNodeControlEvent, SgNodeNativeOptions, SgNodeSchema } from '../../../../models'
import { SgNodeViewDefault } from '../../../node-view'


export namespace SgNodeDefault {

    export type Data = {
        label: string
        icon?: string
        color?: string // valid CSS color
        inlineActions?: InlineAction[]
        actions?: Action[]
        actionsPosition?: SgNodeViewDefault.ActionsPosition
    }

    export type Action = {
        label: string
        icon?: string
        color?: string
        tooltip?: string
        theme?: ThemePalette
        onClick?: () => void
        event?: SgNodeControlEvent
    }

    export type InlineAction = {
        icon: string
        tooltip?: string
        theme?: ThemePalette
        onClick?: () => void
        event?: SgNodeControlEvent
    }

    export type Options = {}

    export const DIMENSIONS_MAP = new Map<SgNodeViewDefault.ActionsPosition, NodeDimension>([
        [
            SgNodeViewDefault.ActionsPosition.right,
            {
                width: 360,
                height: 50,
            },
        ],
        [
            SgNodeViewDefault.ActionsPosition.bottom,
            {
                width: 360,
                height: 94,
            },
        ],
    ])

    export function getDimensions(actionsPosition?: SgNodeViewDefault.ActionsPosition): NodeDimension {
        return DIMENSIONS_MAP.get(actionsPosition ?? SgNodeViewDefault.DEFAULT_POSITION)
    }

    export function toNode(id: string, nodeData: Data, nativeOptions: SgNodeNativeOptions = {}, metadata = {}): SgNode<Data, Options> {
        return {
            id,
            data: {
                ...nodeData,
            },
            metadata,
            nativeOptions: {
                ...{
                    dimension: { ...getDimensions(nodeData?.actionsPosition) }
                },
                ...nativeOptions,
            },
        }
    }

    export function decorateDefaultSchema(schema: SgNodeSchema<any>): SgNodeSchema<any> {
        return {
            ...schema,
            data: {
                label: schema.id,
                ...(schema?.data ?? {}),
            },
        }
    }
}
