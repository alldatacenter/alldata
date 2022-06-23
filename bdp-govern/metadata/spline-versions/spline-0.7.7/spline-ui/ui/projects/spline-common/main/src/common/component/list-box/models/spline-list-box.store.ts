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


import { SplineListBox } from './spline-list-box.models'


export namespace SplineListBoxStore {


    export type State<TRecord, TValue> = {
        selectListOptions: SplineListBox.SelectListOption<TValue>[]
        searchTerm: string
    }

    export function reduceRecords<TRecord, TValue>(
        state: State<TRecord, TValue>,
        records: TRecord[],
        dataMap: SplineListBox.DataMap<TRecord, TValue>,
        searchTerm: string): State<TRecord, TValue> {

        return {
            ...state,
            selectListOptions: calculateSelectListOptions(records, dataMap, searchTerm),
            searchTerm
        }
    }

    function calculateSelectListOptions<TRecord, TValue>(
        records: TRecord[],
        dataMap: SplineListBox.DataMap<TRecord, TValue>,
        searchTerm: string): SplineListBox.SelectListOption<TValue>[] {

        return records
            .map(
                item => SplineListBox.toListOption<TRecord, TValue>(item, dataMap)
            )
            .filter(item => item.label.toLowerCase().includes(searchTerm))
    }
}
