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


import { SdWidgetCardComponent } from './card/sd-widget-card.component'
import { SdWidgetExpansionPanelComponent } from './expansion-panel/sd-widget-expansion-panel.component'
import { SdWidgetJsonComponent } from './json/sd-widget-json.component'
import { SdWidgetRecordsListComponent } from './records-list/sd-widget-records-list.component'
import { SdWidgetSimpleRecordComponent } from './simple-record/sd-widget-simple-record.component'
import { SdWidgetTitleComponent } from './title/sd-widget-title.component'


export const widgetTypesComponents: any[] = [
    SdWidgetSimpleRecordComponent,
    SdWidgetCardComponent,
    SdWidgetTitleComponent,
    SdWidgetRecordsListComponent,
    SdWidgetExpansionPanelComponent,
    SdWidgetJsonComponent
]

export * from './public-api'
