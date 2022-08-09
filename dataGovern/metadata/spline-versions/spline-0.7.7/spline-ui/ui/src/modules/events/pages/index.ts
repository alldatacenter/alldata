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

import { EventOverviewPageComponent } from './event-overview/event-overview.page.component'
import { EventOverviewGraphPageComponent } from './event-overview/graph/event-overview-graph.page.component'
import { EventsListPageComponent } from './events-list/events-list.page.component'


export const pageComponents: any[] = [
    EventsListPageComponent,
    EventOverviewPageComponent,
    EventOverviewGraphPageComponent,
]

export * from './event-overview/event-overview.page.model'
export * from './event-overview/event-overview.page.component'
export * from './event-overview/graph/event-overview-graph.page.component'
export * from './events-list/events-list.page.component'
