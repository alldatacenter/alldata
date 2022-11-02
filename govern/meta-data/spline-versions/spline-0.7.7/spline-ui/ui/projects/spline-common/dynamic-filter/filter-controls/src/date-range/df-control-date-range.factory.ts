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

import { Injectable } from '@angular/core'
import { omit } from 'lodash-es'
import { Observable, of } from 'rxjs'
import { IDynamicFilterControlFactory } from 'spline-common/dynamic-filter'

import { DfControlDateRangeComponent } from './df-control-date-range.component'
import { DfControlDateRange } from './df-control-date-range.models'


@Injectable()
export class DfControlDateRangeFactory implements IDynamicFilterControlFactory<DfControlDateRange.Value, DfControlDateRange.Options> {

    readonly type = DfControlDateRange.TYPE

    readonly componentType = DfControlDateRangeComponent

    createModelFromSchema<TFilterValue extends DfControlDateRange.Value = DfControlDateRange.Value,
        TFilterOptions extends DfControlDateRange.Options = DfControlDateRange.Options,
        TId extends keyof any = any>(
        schema: DfControlDateRange.Schema<TId>,
        defaultValue?: DfControlDateRange.Value): Observable<DfControlDateRange.Model<TId>> {

        // TODO: move config creation to the base abstract class
        const config = { ...omit(schema, 'id', 'type'), defaultValue } as DfControlDateRange.Config
        const controlModel = new DfControlDateRange.Model<TId>(schema.id, config)
        return of(controlModel)
    }

}
