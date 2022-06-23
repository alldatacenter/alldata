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

import { Component, EventEmitter, Input, Output } from '@angular/core'
import { TypeHelpers } from 'spline-utils'

import { SdWidgetSchema, SplineDataViewSchema, SplineDataWidgetEvent } from '../../models'


@Component({
    selector: 'spline-data-view',
    templateUrl: './spline-data-view.component.html',
})
export class SplineDataViewComponent {

    @Input() set dataViewSchema(schema: SplineDataViewSchema) {
        this.widgetSchemas = TypeHelpers.isArray(schema)
            ? schema
            : [schema]
    }

    @Output() event$ = new EventEmitter<SplineDataWidgetEvent>()

    widgetSchemas: SdWidgetSchema[]

    onWidgetEvent($event: SplineDataWidgetEvent): void {
        this.event$.emit($event)
    }

}
