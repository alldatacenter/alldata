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

import { OnInit } from '@angular/core'
import { SplineRecord } from 'spline-utils'

import { DtHeaderCellSchema, IDTHeaderCellValueControl, THeaderCellValue } from '../../../models'
import { BaseCellTypeWithSchemaComponent } from '../../base/base-cell-type-with-schema.component'


export abstract class DtHeaderCellBaseComponent<T, TOptions extends SplineRecord = {}>
    extends BaseCellTypeWithSchemaComponent<T, TOptions, DtHeaderCellSchema<T, TOptions>, THeaderCellValue<T>, THeaderCellValue<TOptions>>
    implements IDTHeaderCellValueControl<T, TOptions>, OnInit {

    protected getSchemaOptions(schema: DtHeaderCellSchema<T, TOptions>): THeaderCellValue<TOptions> | undefined {
        return schema.headerOptions
    }

    protected getSchemaValue(schema: DtHeaderCellSchema<T, TOptions>): THeaderCellValue<T> | undefined {
        return schema.header
    }
}
