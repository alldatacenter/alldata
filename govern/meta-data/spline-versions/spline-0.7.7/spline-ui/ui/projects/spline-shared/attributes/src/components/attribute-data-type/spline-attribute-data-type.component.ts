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

import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core'
import { AttributeDataType, AttributeDataTypeSimple, AttributeDtType } from 'spline-api'


@Component({
    selector: 'spline-attribute-data-type',
    templateUrl: './spline-attribute-data-type.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineAttributeDataTypeComponent implements OnChanges {

    @Input() dataType: AttributeDataType

    dataTypeAlias: string

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.dataType && !!changes.dataType.currentValue) {
            this.dataTypeAlias = this.calculateDataTypeAlias(this.dataType)
        }
    }

    private calculateDataTypeAlias(dataType: AttributeDataType): string {
        switch (dataType.type) {
            case AttributeDtType.Struct:
                return '{ ... }'

            case AttributeDtType.Array:
                return '[ ... ]'

            case AttributeDtType.Simple:
                return (dataType as AttributeDataTypeSimple).name

            default:
                return 'Unknown'
        }
    }

}
