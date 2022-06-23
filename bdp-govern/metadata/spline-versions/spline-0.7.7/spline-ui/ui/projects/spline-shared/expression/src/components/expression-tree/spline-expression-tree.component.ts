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

import { NestedTreeControl } from '@angular/cdk/tree'
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core'
import { MatTreeNestedDataSource } from '@angular/material/tree'
import { AttrSchemasCollection, OpExpression } from 'spline-api'

import { SplineExpressionTree } from '../../models'


@Component({
    selector: 'spline-expression-tree',
    templateUrl: './spline-expression-tree.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineExpressionTreeComponent implements OnInit {

    @Input() expression: OpExpression
    @Input() attrSchemasCollection: AttrSchemasCollection

    treeControl = new NestedTreeControl<SplineExpressionTree.TreeNode>(node => node.children)
    treeDataSource = new MatTreeNestedDataSource<SplineExpressionTree.TreeNode>()

    hasChild = (_: number, node: SplineExpressionTree.TreeNode) => !!node.children && node.children.length > 0

    ngOnInit(): void {

        const tree = SplineExpressionTree.toTree(this.expression, this.attrSchemasCollection)

        this.treeDataSource.data = tree

        // expand first level
        tree
            .filter(node => !!node?.children?.length)
            .forEach(
                node => this.treeControl.expand(node),
            )
    }


}
