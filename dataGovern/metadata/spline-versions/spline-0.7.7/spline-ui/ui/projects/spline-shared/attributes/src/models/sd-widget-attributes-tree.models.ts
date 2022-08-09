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

import { SdWidgetSchema, SplineDataWidgetEvent } from 'spline-common/data-view'
import { DynamicValueProvider } from 'spline-utils'

import { SplineAttributesTree } from './spline-attributes-tree.models'


export namespace SdWidgetAttributesTree {

    export const TYPE = 'AttributesTree'

    export type Data = {
        attributesTree: SplineAttributesTree.Tree
    }

    export type Options = {
        selectedAttributeId?: string
        allowAttrSelection?: boolean // true by default
        allowSearch?: boolean // true by default
        expandAll?: boolean // false by default
        actionIcon?: string
    }

    export function toSchema(attributesTree: SplineAttributesTree.Tree, options?: DynamicValueProvider<Options>): SdWidgetSchema<Data> {
        return {
            type: TYPE,
            data: {
                attributesTree,
            },
            options,
        }
    }

    // EVENTS :: SELECTED ATTR CHANGED

    export const EVENT_TYPE__SELECTED_ATTR_CHANGED = 'AttributesTree.SelectedAttrChanged'
    export type EventSelectedAttrChangedData = {
        attributeId: string | null
    }
    export type EventSelectedAttrChanged = SplineDataWidgetEvent<EventSelectedAttrChangedData>

    export function createEventSelectedAttrChanged(attrId: string | null): EventSelectedAttrChanged {
        return {
            type: EVENT_TYPE__SELECTED_ATTR_CHANGED,
            data: {
                attributeId: attrId
            }
        }
    }

}
