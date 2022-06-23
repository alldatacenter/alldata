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

import { AfterViewInit, Component, Inject } from '@angular/core'
import { MAT_DIALOG_DATA } from '@angular/material/dialog'
import { OperationAttributeLineageType } from 'spline-api'
import { BaseComponent } from 'spline-utils'

import { AttributeLineageDialog } from './attribute-lineage-dialog.models'


@Component({
    selector: 'plan-attribute-lineage-dialog',
    templateUrl: './attribute-lineage-dialog.component.html',
    styleUrls: ['./attribute-lineage-dialog.component.scss'],
})
export class AttributeLineageDialogComponent extends BaseComponent implements AfterViewInit {

    isInitialized = false
    readonly dialogTitle: string

    constructor(@Inject(MAT_DIALOG_DATA) public data: AttributeLineageDialog.Data) {
        super()

        this.dialogTitle = data.lineageType === OperationAttributeLineageType.Lineage
            ? 'PLANS.ATTR_LINEAGE_DETAILS__DIALOG__TITLE__LINEAGE'
            : 'PLANS.ATTR_LINEAGE_DETAILS__DIALOG__TITLE__IMPACT'
    }

    ngAfterViewInit(): void {
        const domRelaxationTime = 250
        setTimeout(() => this.isInitialized = true, domRelaxationTime)
    }
}
