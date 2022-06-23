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


import { PageResponse, SplineDateRangeValue } from 'spline-utils'

import { ExecutionEvent, ExecutionEventDto, toExecutionEvent } from './execution-event.models'


export type ExecutionEventsPageResponseDto =
    & PageResponse<ExecutionEventDto>
    &
    {
        totalDateRange: number[]
    }

export type ExecutionEventsPageResponse =
    & PageResponse<ExecutionEvent>
    &
    {
        dateRangeBounds: SplineDateRangeValue
    }

export function toExecutionEventsPageResponse(entity: ExecutionEventsPageResponseDto): ExecutionEventsPageResponse {
    return {
        items: entity.items.map(toExecutionEvent),
        totalCount: entity.totalCount,
        dateRangeBounds: {
            dateFrom: new Date(entity.totalDateRange[0]),
            dateTo: new Date(entity.totalDateRange[1]),
        },
    }
}
