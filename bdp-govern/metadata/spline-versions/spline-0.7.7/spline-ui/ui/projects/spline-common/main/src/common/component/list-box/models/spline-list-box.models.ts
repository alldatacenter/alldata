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

import { StringHelpers } from 'spline-utils'


export namespace SplineListBox {

    export type SelectListOption<TValue> = {
        value: TValue
        label: string
    }

    export type DataMap<TRecord, TValue = TRecord> = {
        compareValueWith?: (valueLeft: TValue, valueRight: TValue) => boolean
        trackBy?: (value: TValue) => string | number
        extractValue?: (record: TRecord) => TValue
        extractLabel?: (record: TRecord) => string
        valueToString?: (value: TValue) => string
    }

    export function getDefaultDataMap(): DataMap<any, any> {
        return {
            compareValueWith: record => record,
            trackBy: record => record,
            extractValue: (record) => record,
            extractLabel: (record) => record,
            valueToString: (record) => StringHelpers.toStringValue(record)
        }
    }

    export function toListOption<TRecord, TValue>(
        record: TRecord,
        dataMap: DataMap<TRecord, TValue>): SelectListOption<TValue> {

        return {
            value: dataMap.extractValue(record),
            label: dataMap.extractLabel(record),
        }
    }

    export type SimpleListRecord<TValue> = {
        value: TValue
        label: string
    }

    export function getDefaultSimpleDataMap<TValue>(): DataMap<SimpleListRecord<TValue>, TValue> {
        return {
            extractValue: (record) => record.value,
            extractLabel: (record) => record.label
        }
    }
}
