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

import { ThemePalette } from '@angular/material/core/common-behaviors/color'
import { NodeDimension } from '@swimlane/ngx-graph/lib/models/node.model'

import { SgNode, SgNodeNativeOptions } from '../../../../models'


export namespace SgNodeCircleButton {

    export const TYPE = 'circle_button'

    export type Data = {
        eventName: string
        icon?: string
        tooltip?: string
        theme?: ThemePalette
    }

    export type Options = {}

    export const DEFAULT_DIMENSIONS: Readonly<NodeDimension> = Object.freeze({
        width: 40,
        height: 40,
    })

    export function toNode(id: string, nodeData: Data, nativeOptions: SgNodeNativeOptions = {}): SgNode<Data, Options> {
        return {
            id,
            type: TYPE,
            data: {
                ...nodeData,
            },
            nativeOptions: {
                dimension: {...DEFAULT_DIMENSIONS},
                ...nativeOptions,
            },
            disallowSelection: true
        }
    }
}
