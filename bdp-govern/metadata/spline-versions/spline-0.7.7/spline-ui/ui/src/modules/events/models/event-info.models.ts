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

import { SplineColors } from 'spline-common'
import { SdWidgetCard, SdWidgetSimpleRecord, SplineDataViewSchema } from 'spline-common/data-view'
import { DateTimeHelpers } from 'spline-utils'


export type EventInfo = {
    id: string
    name: string
    executedAt: Date
    applicationId: string
    executionPlan?: {
        id: string
        name: string
    }
}

export function toEventInfoDataViewSchema(eventInfo: EventInfo): SplineDataViewSchema {
    return [
        SdWidgetCard.toSchema(
            {
                color: SplineColors.PINK,
                icon: 'play_arrow',
                title: eventInfo.name,
                iconTooltip: 'EVENTS.EVENT_INFO__TOOLTIP',
            },
            [
                SdWidgetSimpleRecord.toSchema([
                    {
                        label: 'EVENTS.EVENT_INFO__DETAILS__EXECUTED_AT',
                        value: DateTimeHelpers.toString(eventInfo.executedAt),
                    },
                    {
                        label: 'EVENTS.EVENT_INFO__DETAILS__EVENT_ID',
                        value: eventInfo.id,
                    },
                    {
                        label: 'EVENTS.EVENT_INFO__DETAILS__APPLICATION_ID',
                        value: eventInfo.applicationId,
                    },
                ])
            ]
        ),
    ]
}
