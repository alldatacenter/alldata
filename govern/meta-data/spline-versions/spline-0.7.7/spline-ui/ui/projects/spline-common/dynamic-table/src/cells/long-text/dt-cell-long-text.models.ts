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

import { DtCellValueSchema, TCellValue } from '../../core'


export namespace DtCellLongText {

    export const TYPE = 'LongText'

    export interface Value {
        text: string
        tooltip?: string
    }

    export function getColSchema(value: TCellValue<Value>): Partial<DtCellValueSchema<Value>> {
        return {
            value,
            type: TYPE,
            layout: {
                styles: {
                    width: '180px',
                },
            },
        }
    }

}
