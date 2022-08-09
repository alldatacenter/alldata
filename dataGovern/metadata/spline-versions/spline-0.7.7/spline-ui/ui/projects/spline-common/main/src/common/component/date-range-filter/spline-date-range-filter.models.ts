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

import moment from 'moment'
import { DateTimeHelpers, SplineDateRangeValue } from 'spline-utils'


export namespace SplineDateRangeFilter {

    export type Value = SplineDateRangeValue

    export type ValueMoment = {
        dateFrom: moment.Moment
        dateTo: moment.Moment
    }

    export function valueToString(value: Value): string {
        const dateFromString = DateTimeHelpers.toString(value.dateFrom, DateTimeHelpers.FULL_DATE)
        const dateToString = DateTimeHelpers.toString(value.dateTo, DateTimeHelpers.FULL_DATE)

        return `${dateFromString} - ${dateToString}`
    }

    export type State = {
        value: Value
        valueMoment: SplineDateRangeValue<moment.Moment>
        valueString: string | null
    }

    export function getDefaultState(): State {
        return {
            value: null,
            valueMoment: null,
            valueString: null
        }
    }

    export function reduceValueChanged(state: State, value: Value | null): State {

        if (!value) {
            return {
                ...getDefaultState()
            }
        }

        return {
            ...state,
            value: {
                ...value
            },
            valueMoment: {
                dateFrom: moment(value.dateFrom),
                dateTo: moment(value.dateTo),
            },
            valueString: valueToString(value)
        }
    }

}
