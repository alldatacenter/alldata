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

import { SdWidgetSchema } from '../../../models'


export namespace SdWidgetTitle {

    export const TYPE = 'Title'

    export type Data = {
        text: string
    }

    export type TitleType = 'sectionTitle'
    export const TitleType = {
        sectionTitle: 'sectionTitle' as TitleType,
    }

    export type Options = {
        type?: TitleType
        styles?: Partial<CSSStyleDeclaration>
    }

    export const DEFAULT_OPTIONS: Options = Object.freeze<Options>({
        type: TitleType.sectionTitle
    })

    export function toSchema(text: string, options: Options = DEFAULT_OPTIONS): SdWidgetSchema<Data> {
        return {
            type: TYPE,
            data: {
                text,
            },
            options,
        }
    }

}
