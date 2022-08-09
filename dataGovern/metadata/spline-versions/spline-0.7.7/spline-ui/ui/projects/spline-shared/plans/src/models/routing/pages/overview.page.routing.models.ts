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

import { PlansRouting } from 'spline-shared/plans'
import { RouterNavigation } from 'spline-utils'


export namespace PlanOverviewPageRouting {

    export enum PathFragment {
        overview = 'overview'
    }

    export enum QueryParam {
        ExecutionPlanId = 'planId',
        ExecutionEventId = 'eventId',
        SelectedNodeId = 'nodeId',
        SelectedAttributeId = 'attributeId',
    }

    export const NG_ROUTER_ALIAS = `${PathFragment.overview}/:${QueryParam.ExecutionPlanId}`

    export function getPath(executionPlanId: string): any[] {
        return [PlansRouting.BASE_PATH, PathFragment.overview, executionPlanId]
    }

    export function getNavigationInfo(executionPlanId: string,
                                      executionEventId?: string,
                                      selectedNodeId?: string,
                                      selectedAttributeId?: string): RouterNavigation.NavigationInfo {

        const queryParams = {
            [QueryParam.ExecutionEventId]: executionEventId,
            [QueryParam.SelectedNodeId]: selectedNodeId,
            [QueryParam.SelectedAttributeId]: selectedAttributeId
        }

        return {
            path: getPath(executionPlanId),
            extras: {
                queryParams
            }
        }
    }
}
