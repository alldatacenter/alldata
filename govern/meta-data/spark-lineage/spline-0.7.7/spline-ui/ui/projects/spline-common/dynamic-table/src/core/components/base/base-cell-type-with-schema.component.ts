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

import { BaseCellTypeComponent } from './base-cell-type.component'


@Component({
    selector: 'dt-base-cell-type-with-schema',
    template: ''
})
export abstract class BaseCellTypeWithSchemaComponent<T, TOptions, TSchema, TSchemaValue, TSchemaOptions>
    extends BaseCellTypeComponent<T, TOptions> implements OnInit {

    @Input() schema: TSchema

    ngOnInit(): void {
        this.initValueFromSchema(this.schema)
        this.initOptionsFromSchema(this.schema)
    }

    protected initValueFromSchema(schema: TSchema): void {
        const schemaValue = this.getSchemaValue(schema)
        if (schemaValue !== undefined) {
            const value = typeof schemaValue === 'function'
                ? (schemaValue as Function)()
                : schemaValue
            this.initValueFromSource(this.value$, value)
        }
    }

    protected initOptionsFromSchema(schema: TSchema): void {
        const schemaValue = this.getSchemaOptions(schema)
        if (schemaValue !== undefined) {
            const value = typeof schemaValue === 'function'
                ? (schemaValue as Function)()
                : schemaValue
            this.initValueFromSource(this.options$, value)
        }
    }

    protected abstract getSchemaOptions(schema: TSchema): TSchemaOptions | undefined;

    protected abstract getSchemaValue(schema: TSchema): TSchemaValue | undefined;

}


