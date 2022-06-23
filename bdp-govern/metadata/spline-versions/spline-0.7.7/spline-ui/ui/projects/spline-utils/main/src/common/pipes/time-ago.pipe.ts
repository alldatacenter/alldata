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

import { Pipe, PipeTransform } from '@angular/core'
import { isDate, isMoment, Moment } from 'moment'

import { TypeHelpers } from '../models'
import isNumber = TypeHelpers.isNumber


const TIME_INTERVAL_UNITS = {
    year: 31536000,
    month: 2592000,
    week: 604800,
    day: 86400,
    hour: 3600,
    minute: 60,
    second: 1
}

@Pipe({ name: 'timeAgo' })
export class TimeAgoPipe implements PipeTransform {

    private static toText(timestamp: number): string {
        const seconds = (Date.now() - timestamp) / 1000
        if (seconds < 29) {
            return 'Just now'
        }

        for (const unit of Object.keys(TIME_INTERVAL_UNITS)) {
            const n = Math.floor(seconds / TIME_INTERVAL_UNITS[unit])
            if (n > 0) {
                return (n === 1)
                    ? `${n} ${unit} ago` // singular
                    : `${n} ${unit}s ago` // plural
            }
        }
    }

    transform(value: number | Date | Moment) {
        if (isDate(value)) {
            return TimeAgoPipe.toText(value.getTime())
        }
        if (isMoment(value)) {
            return TimeAgoPipe.toText(+value)
        }
        else if (isNumber(value)) {
            return TimeAgoPipe.toText(value)
        }
        return value
    }
}
