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
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core'
import { MatTreeNestedDataSource } from '@angular/material/tree'
import { BehaviorSubject } from 'rxjs'
import { skip, takeUntil } from 'rxjs/operators'
import { BaseComponent } from 'spline-utils'

import { SplineAttributesTree } from '../../models'


@Component({
    selector: 'spline-attributes-tree',
    templateUrl: './spline-attributes-tree.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineAttributesTreeComponent extends BaseComponent implements OnChanges {

    readonly defaultActionIcon = 'local_offer'

    @Input() attributesTree: SplineAttributesTree.Tree
    @Input() selectedAttributeId: string
    @Input() allowAttrSelection = true
    @Input() allowSearch = true
    @Input() actionIcon = this.defaultActionIcon
    @Input() expandAll = false

    @Input() set searchTerm(value: string) {
        this.searchTerm$.next(value)
    }



    @Output() selectedAttributeChanged$ = new EventEmitter<{ attributeId: string }>()

    readonly treeControl = new NestedTreeControl<SplineAttributesTree.TreeNode>(node => node.children)
    readonly treeDataSource = new MatTreeNestedDataSource<SplineAttributesTree.TreeNode>()

    readonly searchTerm$ = new BehaviorSubject<string>('')

    constructor() {
        super()
        this.searchTerm$
            .pipe(
                skip(1),
                takeUntil(this.destroyed$)
            )
            .subscribe((searchTerm) => {
                this.treeDataSource.data = searchTerm?.length > 0
                    ? this.attributesTree.filter(treeNode => treeNode.name.toLowerCase().includes(searchTerm))
                    : this.attributesTree
            })
    }

    hasChild = (_: number, node: SplineAttributesTree.TreeNode) => !!node.children && node.children.length > 0

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.attributesTree?.currentValue) {
            this.treeDataSource.data = changes.attributesTree.currentValue
            this.treeControl.dataNodes = changes.attributesTree.currentValue
        }

        if (changes?.expandAll && changes.expandAll.currentValue) {
            this.treeControl.expandAll()
        }
    }

    onHighlightBtnClicked($event: MouseEvent, nodeId: string): void {
        $event.stopPropagation()
        if (this.allowAttrSelection) {
            this.selectedAttributeId = nodeId
        }

        // emit event anyway
        this.selectedAttributeChanged$.emit({
            attributeId: nodeId
        })
    }

    onSearch(searchTerm: string): void {
        this.searchTerm$.next(searchTerm)
    }
}
