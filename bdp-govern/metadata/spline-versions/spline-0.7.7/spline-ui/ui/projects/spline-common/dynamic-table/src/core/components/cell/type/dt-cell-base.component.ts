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

import { Component, Input, OnInit } from '@angular/core'
import { SplineRecord } from 'spline-utils'

import { DtCellValueSchema, IDTCellValueControl, TCellValue } from '../../../models'
import { BaseCellTypeWithSchemaComponent } from '../../base/base-cell-type-with-schema.component'

@Component({
    selector: 'dt-cell-base',
    template: ''
})
export abstract class DtCellBaseComponent<T, TOptions extends SplineRecord = {}>
    extends BaseCellTypeWithSchemaComponent<T, TOptions, DtCellValueSchema<T, TOptions>, TCellValue<T>, TCellValue<TOptions>>
    implements IDTCellValueControl<T, TOptions>, OnInit {

    @Input() rowData: any;

    ngOnInit(): void {
        this.initOptionsFromSchema(this.schema, this.rowData)
        this.initValueFromSchema(this.schema, this.rowData)
    }

    protected getSchemaOptions(schema: DtCellValueSchema<T, TOptions>): TCellValue<TOptions> | undefined {
        return schema.options
    }

    protected getSchemaValue(schema: DtCellValueSchema<T, TOptions>): TCellValue<T> | undefined {
        return schema.value
    }

    protected initValueFromSchema(schema: DtCellValueSchema<T, TOptions>, rowData?: any): void {
        const schemaValue = this.getSchemaValue(schema)
        if (schemaValue !== undefined) {
            const value = typeof schemaValue === 'function'
                ? (schemaValue as Function)(rowData)
                : schemaValue

            this.initValueFromSource(this.value$, value)

        }
        else if (rowData[schema.id] !== undefined) {
            this.value$.next(rowData[schema.id])
        }
    }

    protected initOptionsFromSchema(schema: DtCellValueSchema<T, TOptions>, rowData?: any): void {
        const schemaValue = this.getSchemaOptions(schema)
        if (schemaValue !== undefined) {
            const value = typeof schemaValue === 'function'
                ? (schemaValue as Function)(rowData)
                : schemaValue

            this.initValueFromSource(this.options$, value)
        }
    }
}
