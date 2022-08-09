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

import { AttrSchemasCollection, OpExpression } from 'spline-api'
import { SdWidgetSchema } from 'spline-common/data-view'
import { DynamicValueProvider } from 'spline-utils'


export namespace SdWidgetExpression {

    export const TYPE = 'Expression'

    export type Data = {
        expression: OpExpression
        attrSchemasCollection: AttrSchemasCollection
        prefix?: string
        suffix?: string
    }

    export type Options = {}

    export function toSchema(data: DynamicValueProvider<Data>): SdWidgetSchema<Data> {
        return {
            type: TYPE,
            data,
        }
    }

}
