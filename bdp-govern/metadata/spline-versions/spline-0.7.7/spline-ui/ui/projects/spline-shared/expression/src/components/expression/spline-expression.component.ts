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

import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core'
import { MatDialog } from '@angular/material/dialog'
import { AttrSchemasCollection, OpExpression } from 'spline-api'

import { SplineExpressionTreeDialog, SplineExpressionValue } from '../../models'
import { SplineExpressionTreeDialogComponent } from '../expression-tree-dialog/spline-expression-tree-dialog.component'


@Component({
    selector: 'spline-expression',
    templateUrl: './spline-expression.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineExpressionComponent implements OnInit {

    @Input() expression: OpExpression
    @Input() attrSchemasCollection: AttrSchemasCollection
    @Input() prefix: string
    @Input() suffix: string
    @Input() showActions = true

    expressionString: string

    constructor(private readonly dialog: MatDialog) {
    }

    ngOnInit(): void {
        this.expressionString = SplineExpressionValue.expressionToString(this.expression, this.attrSchemasCollection)
    }

    onShowTreeViewBtnClicked(): void {
        this.dialog.open<SplineExpressionTreeDialogComponent, SplineExpressionTreeDialog.Data>(
            SplineExpressionTreeDialogComponent,
            {
                data: {
                    expression: this.expression,
                    attrSchemasCollection: this.attrSchemasCollection,
                    prefix: this.prefix,
                    suffix: this.suffix,
                },
                minWidth: '700px',
                maxWidth: '1400px',
                autoFocus: false,
            },
        )
    }
}
