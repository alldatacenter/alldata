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

import { DfControlSelectComponent } from '../components/df-control-select/df-control-select.component'
import { DfControlSelect } from '../models/df-control-select.models'


@Injectable()
export class DfControlSelectFactory implements IDynamicFilterControlFactory<DfControlSelect.Value, DfControlSelect.Options> {

    readonly type = DfControlSelect.TYPE

    readonly componentType = DfControlSelectComponent

    createModelFromSchema<TFilterValue extends DfControlSelect.Value = DfControlSelect.Value,
        TFilterOptions extends DfControlSelect.Options = DfControlSelect.Options,
        TId extends keyof any = any>(
        schema: DfControlSelect.Schema<TId>,
        defaultValue?: DfControlSelect.Value): Observable<DfControlSelect.Model<TId>> {

        // TODO: move config creation to the base abstract class
        const config = { ...omit(schema, 'id', 'type'), defaultValue } as DfControlSelect.Config
        const controlModel = new DfControlSelect.Model<TId>(schema.id, config)
        return of(controlModel)
    }


}
