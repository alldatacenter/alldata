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

import { ChangeDetectionStrategy, Component } from '@angular/core'
import { get } from 'lodash-es'
import { BehaviorSubject } from 'rxjs'

import { DtCellValueSchema } from '../../../../models'
import { DtCellBaseComponent } from '../dt-cell-base.component'


@Component({
    selector: 'dt-cell-default',
    templateUrl: 'dt-cell-default.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DtCellDefaultComponent extends DtCellBaseComponent<string> {
    value$ = new BehaviorSubject<string>('');

    protected initValueFromSchema(schema: DtCellValueSchema<string>, rowData: any): void {

        if (schema.value !== undefined) {

            const value = typeof schema.value === 'function'
                ? (schema.value as Function)(rowData)
                : schema.value

            this.initValueFromSource<string>(
                this.value$, this.decorateNullValue(value),
            )

        }
        else if (get(rowData, schema.id, undefined) !== undefined) {
            this.value$.next(
                this.decorateNullValue(rowData[schema.id]),
            )
        }
    }

    protected decorateNullValue(value: string): string {
        return value !== null && value !== undefined
            ? value
            : ''
    }
}

