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

import { SplineCardHeader } from 'spline-common'
import { TypeHelpers } from 'spline-utils'

import { SdWidgetSchema, SplineDataViewSchema } from '../../../models'


export namespace SdWidgetCard {

    export const TYPE = 'Card'

    export type HeaderData = {
        title: string
        icon: string
        iconTooltip?: string
        color?: string
        actions?: SplineCardHeader.Action[]
    }

    export type ContentData = {
        dataSchema: SplineDataViewSchema
    }

    export type Data = {
        cardCssClassList?: string | string[]
        header?: HeaderData
        content?: ContentData
    }

    export function toSchema(header?: HeaderData, contentDataSchema?: SplineDataViewSchema): SdWidgetSchema<Data> {
        return {
            type: TYPE,
            data: {
                header,
                content: contentDataSchema ? { dataSchema: contentDataSchema } : undefined,
            },
        }
    }

    export function toContentOnlySchema(contentDataSchema: SplineDataViewSchema): SdWidgetSchema<Data> {
        return {
            type: TYPE,
            data: {
                content: contentDataSchema
                    ? { dataSchema: TypeHelpers.isArray(contentDataSchema) ? contentDataSchema : [contentDataSchema] }
                    : undefined,
            },
        }
    }

}
