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

import { SplineDateRangeFilter } from 'spline-common'
import { BaseDynamicFilterControlModel, DynamicFilterControlSchema } from 'spline-common/dynamic-filter'
import { SplineDateRangeValue } from 'spline-utils'


export namespace DfControlDateRange {

    export const TYPE = 'DfControlDateRange'

    export type Value = SplineDateRangeFilter.Value

    export type Options = {}

    export type Schema<TId extends keyof any = any> =
        & DynamicFilterControlSchema<Value, Options, TId>
        &
        {
            icon?: string
            label?: string
            bounds?: SplineDateRangeValue | null
        }

    export function getSchema<TId extends keyof any = any>(
        label?: string,
        icon?: string,
        bounds?: SplineDateRangeValue | null): Partial<Schema> {

        return {
            type: TYPE,
            label,
            icon,
            bounds
        }
    }

    export type Config = Omit<Schema, 'id' | 'type'>

    export class Model<TId extends keyof any = any> extends BaseDynamicFilterControlModel<Value, Options, TId> {

        readonly type = TYPE

        readonly label: string
        readonly icon: string

        bounds: SplineDateRangeValue | null = null

        constructor(id: TId, config?: Config) {
            super(id, config)

            this.label = config?.label
            this.icon = config?.icon
            this.bounds = config?.bounds || null
        }
    }

}
