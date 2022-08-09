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

import { MatAccordionTogglePosition } from '@angular/material/expansion/accordion-base'

import { SdWidgetSchema, SplineDataViewSchema } from '../../../models'


export namespace SdWidgetExpansionPanel {

    export const TYPE = 'ExpansionPanel'

    export type HeaderData = {
        title: string
        description?: string
        icon?: string
        iconColor?: string
    }

    export type ContentData = {
        dataSchema: SplineDataViewSchema
    }

    export type Data = {
        header?: HeaderData
        content?: ContentData
    }

    export type Options = {
        expanded?: boolean // TRUE by default
        hideToggle?: boolean
        disabled?: boolean
        togglePosition?: MatAccordionTogglePosition
    }

    export function toSchema(header: HeaderData, contentDataSchema?: SplineDataViewSchema, options?: Options): SdWidgetSchema<Data> {
        return {
            type: TYPE,
            data: {
                header,
                content: contentDataSchema ? { dataSchema: contentDataSchema } : undefined,
            },
            options
        }
    }

}
