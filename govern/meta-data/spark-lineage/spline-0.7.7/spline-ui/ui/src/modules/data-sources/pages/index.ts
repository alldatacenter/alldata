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

import { DataSourcesListPageComponent } from './list/data-sources-list.page.component'
import { DataSourceOverviewPageComponent } from './overview/data-source-overview.page.component'
import { DsOverviewHistoryPageComponent } from './overview/history/ds-overview-history.page.component'


export const pageComponents: any[] = [
    DataSourcesListPageComponent,
    DataSourceOverviewPageComponent,
    DsOverviewHistoryPageComponent,
]

export * from './list/data-sources-list.page.component'
export * from './overview/data-source-overview.page.component'
export * from './overview/history/ds-overview-history.page.component'
