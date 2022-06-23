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

import {
    Component,
    ContentChildren,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    QueryList,
    SimpleChanges,
    ViewChild
} from '@angular/core'
import { isEqual } from 'lodash-es'
import { Subject } from 'rxjs'
import { distinctUntilChanged, filter, map, takeUntil } from 'rxjs/operators'
import {
    SgControlPanelSectionDirective,
    SgData,
    SgNodeEvent,
    SgNodeSchema,
    SgRelations,
    SplineGraphComponent
} from 'spline-common/graph'
import { BaseLocalStateComponent } from 'spline-utils'

import { SgNodeControl } from '../../models'

import { SgContainer } from './sg-container.models'
import NodeView = SgNodeControl.NodeView


@Component({
    selector: 'sg-container',
    templateUrl: './sg-container.component.html'
})
export class SgContainerComponent extends BaseLocalStateComponent<SgContainer.State> implements OnInit, OnChanges {

    @ViewChild(SplineGraphComponent) splineGraph: SplineGraphComponent
    @ContentChildren(SgControlPanelSectionDirective) controlPanelExtraSections: QueryList<SgControlPanelSectionDirective>

    @Input() graphData: SgData
    @Input() selectedNodeId: string | null
    @Input() targetNodeId: string | null
    @Input() graphNodeView: NodeView = NodeView.Detailed
    @Input() showGraphNodeView = true

    @Output() nodeEvent$ = new EventEmitter<SgNodeEvent>()
    @Output() nodeDoubleClick$ = new EventEmitter<{ nodeSchema: SgNodeSchema }>()
    @Output() nodeSelectionChange$ = new EventEmitter<{ nodeId: string | null }>()
    @Output() highlightedRelationsNodesIdsChange$ = new EventEmitter<{ nodeIds: string[] | null }>()
    @Output() graphNodeViewChange$ = new EventEmitter<{ nodeView: NodeView }>()

    readonly focusNode$ = new Subject<string>()

    constructor() {
        super()

        this.state$
            .pipe(
                map(state => state && state?.selectedNodeId),
                distinctUntilChanged(),
                takeUntil(this.destroyed$),
                filter(nodeId => nodeId !== this.selectedNodeId)
            )
            .subscribe(
                nodeId => this.nodeSelectionChange$.emit({ nodeId })
            )

        this.state$
            .pipe(
                map(state => state && state?.highlightedRelationsNodesIds),
                distinctUntilChanged((left, right) => isEqual(left, right)),
                takeUntil(this.destroyed$),
            )
            .subscribe(
                nodeIds => this.highlightedRelationsNodesIdsChange$.emit({ nodeIds })
            )
    }

    ngOnChanges(changes: SimpleChanges): void {
        ['selectedNodeId']
            .forEach(key => {
                const currentChangeRef = changes[key]
                if (currentChangeRef && !currentChangeRef.isFirstChange()) {
                    this.updateState({
                        [key]: currentChangeRef.currentValue
                    })
                }
            })
    }

    ngOnInit(): void {
        // init default state
        this.updateState({
            selectedNodeId: this.selectedNodeId,
            highlightedRelationsNodesIds: null,
        })
    }

    refresh(): void {
        this.graphData = {... this.graphData}
    }

    focusNode(nodeId: string): void {
        this.focusNode$.next(nodeId)
    }

    highlightNodeRelations(nodeId: string | null): void {
        if (nodeId) {
            this.processNodeHighlightAction(nodeId)
        }
        else {
            this.updateState({
                highlightedRelationsNodesIds: null
            })
        }
    }

    highlightSpecificRelations(nodeIds): void {
        this.updateState({
            highlightedRelationsNodesIds: nodeIds
        })
    }

    onNodeSelected(nodeSchema: SgNodeSchema | null): void {
        this.nodeSelectionChange$.emit({ nodeId: nodeSchema ? nodeSchema.id : null })
    }

    onToggleAllRelationsBtnClicked(): void {
        this.highlightNodeRelations(null)
    }


    onNodeFocus(nodeId: string): void {
        this.focusNode(nodeId)
    }

    onNodeHighlightRelations(nodeId: string): void {
        this.highlightNodeRelations(nodeId)
    }

    onGraphNodeEvent($event: SgNodeEvent): void {
        switch ($event.event.type) {
            case SgNodeControl.NodeControlEvent.Focus:
                this.onNodeFocus($event.nodeSchema.id)
                break

            case SgNodeControl.NodeControlEvent.HighlightRelations:
                this.onNodeHighlightToggleRelations($event.nodeSchema.id)
                break
        }

        this.nodeEvent$.emit($event)
    }

    onNodeDoubleClick(nodeSchema: SgNodeSchema): void {
        this.nodeDoubleClick$.emit({ nodeSchema })
    }

    onGraphNodeViewChanged(checked: boolean): void {
        this.graphNodeViewChange$.emit({
            nodeView: checked ? NodeView.Compact : NodeView.Detailed
        })
    }

    private onNodeHighlightToggleRelations(nodeId: string): void {
        this.processNodeHighlightAction(nodeId)
    }

    private processNodeHighlightAction(nodeId: string): void {
        const highlightedRelationsNodesIds = SgRelations.toggleSelection(
            this.state.highlightedRelationsNodesIds ?? [],
            nodeId,
            this.graphData.links,
        )
        this.updateState({
            highlightedRelationsNodesIds: highlightedRelationsNodesIds.length > 0 ? highlightedRelationsNodesIds : null
        })
    }
}
