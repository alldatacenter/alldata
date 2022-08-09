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

import { BehaviorSubject, Observable } from 'rxjs'
import { SplineListBox } from 'spline-common'
import { BaseDynamicFilterControlModel, DynamicFilterControlSchema } from 'spline-common/dynamic-filter'


export namespace DfControlSelect {

    export const TYPE = 'DfControlSelect'

    export type Value<TSelectValue = unknown> = TSelectValue[]

    export type Options<TRecord = unknown, TSelectValue = TRecord> = {
        dataMap?: SplineListBox.DataMap<TRecord, TSelectValue>
        valueLabelAllSelected?: string
    }

    export type Schema<TId extends keyof any = any, TRecord = unknown, TSelectValue = TRecord> =
        & DynamicFilterControlSchema<Value<TSelectValue>, Options<TRecord, TSelectValue>, TId>
        &
        {
            label: string
            icon: string
            records?: TRecord[]
        }

    export type Config<TRecord = unknown, TValue = TRecord> =
        Omit<Schema<keyof any, TRecord, TValue>, 'id' | 'type'>

    export class Model<TId extends keyof any = any, TRecord = unknown, TSelectValue = TRecord>
        extends BaseDynamicFilterControlModel<Value<TSelectValue>, Options<TRecord, TSelectValue>, TId> {
        readonly type = TYPE

        readonly label: string
        readonly icon: string
        readonly records$: Observable<TRecord[]>

        private readonly _records$ = new BehaviorSubject<TRecord[]>([])

        constructor(id: TId, config: Config<TRecord, TSelectValue>) {
            super(id, config)

            if (config.records) {
                this.records = config.records
            }

            this.icon = config.icon
            this.label = config.label

            this.records$ = this._records$
        }

        get records(): TRecord[] {
            return this._records$.getValue()
        }

        set records(records: TRecord[]) {
            this._records$.next(records)
        }
    }

}
