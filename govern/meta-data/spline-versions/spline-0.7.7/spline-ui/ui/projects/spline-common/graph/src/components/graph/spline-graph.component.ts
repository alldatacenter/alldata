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
    OnDestroy,
    Output,
    QueryList,
    SimpleChanges,
    ViewChild
} from '@angular/core'
import { GraphComponent } from '@swimlane/ngx-graph'
import { Edge } from '@swimlane/ngx-graph/lib/models/edge.model'
import * as fromD3Shape from 'd3-shape'
import { BehaviorSubject, Observable, Subject } from 'rxjs'
import { buffer, debounceTime, takeUntil } from 'rxjs/operators'
import { BaseComponent } from 'spline-utils'

import { SgControlPanelSectionDirective } from '../../directives/control-panel-action/sg-control-panel-section.directive'
import {
    SgData,
    SgLayoutSettings,
    SgNativeNode,
    SgNodeControlEvent,
    SgNodeEvent,
    SgNodeSchema,
    SG_DEFAULT_LAYOUT_SETTINGS,
    toSgNativeNode,
} from '../../models'


@Component({
    selector: 'spline-graph',
    templateUrl: './spline-graph.component.html',
})
export class SplineGraphComponent extends BaseComponent implements OnChanges, OnDestroy {

    @ViewChild(GraphComponent) ngxGraphComponent: GraphComponent
    @ContentChildren(SgControlPanelSectionDirective) controlPanelExtraSections: QueryList<SgControlPanelSectionDirective>

    @Input() graphData: SgData
    @Input() curve: fromD3Shape.CurveFactoryLineOnly | fromD3Shape.CurveFactory = fromD3Shape.curveBasis
    @Input() layoutSettings: SgLayoutSettings = SG_DEFAULT_LAYOUT_SETTINGS
    @Input() focusNode$: Observable<string>

    @Input() set selectedNodeId(nodeId: string) {
        if (nodeId !== this._selectedNodeId) {
            this._selectedNodeId = nodeId
        }
    }

    @Input() set targetNodeId(nodeId: string) {
        if (nodeId !== this._targetNodeId) {
            this._targetNodeId = nodeId
        }
    }

    @Output() substrateClick$ = new EventEmitter<{ mouseEvent: MouseEvent }>()
    @Output() nodeClick$ = new EventEmitter<{ nodeSchema: SgNodeSchema; mouseEvent: MouseEvent }>()
    @Output() nodeDoubleClick$ = new EventEmitter<{ nodeSchema: SgNodeSchema; mouseEvent: MouseEvent }>()
    @Output() nodeEvent$ = new EventEmitter<SgNodeEvent>()
    @Output() nodeSelectionChange$ = new EventEmitter<{ nodeSchema: SgNodeSchema | null }>()


    _selectedNodeId: string
    _focusedNodeId: string
    _targetNodeId: string

    readonly defaultNodeWidth = 350
    readonly defaultNodeHeight = 50

    readonly nativeGraphData$ = new BehaviorSubject<{ nodes: SgNativeNode[]; links: Edge[] }>(null)

    private focusedNodeTimer: any
    private nodeClicksStream$ = new Subject<{ node: SgNativeNode; mouseEvent: MouseEvent }>()

    constructor() {
        super()
        this.initNodeClicksEvents()
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.graphData && changes.graphData.currentValue) {
            const currentGraphData: SgData = changes.graphData.currentValue
            this.nativeGraphData$.next({
                nodes: currentGraphData.nodes.map(toSgNativeNode),
                links: changes.graphData.currentValue.links
            })
        }
        if (changes?.focusNode$ && changes.focusNode$.currentValue) {
            changes.focusNode$.currentValue
                .pipe(
                    takeUntil(this.destroyed$),
                )
                .subscribe((nodeId) => this.onFocusNode(nodeId))
        }
    }

    ngOnDestroy() {
        super.ngOnDestroy()
        this.nodeClicksStream$.complete()
        this.nativeGraphData$.complete()
    }

    onNodeEvent(node: SgNativeNode, event: SgNodeControlEvent<any>): void {
        this.nodeEvent$.emit({
            nodeSchema: node,
            event,
        })
    }

    onNodeClicked(node: SgNativeNode, mouseEvent: MouseEvent): void {
        mouseEvent.stopPropagation()

        this.nodeClicksStream$.next({
            node,
            mouseEvent,
        })
    }

    onGraphClicked($event: MouseEvent): void {
        this.substrateClick$.emit({ mouseEvent: $event })
    }

    private initNodeClicksEvents(): void {
        const doubleClickDelayInMs = 150
        const buff$ = this.nodeClicksStream$.pipe(
            debounceTime(doubleClickDelayInMs),
        )

        this.nodeClicksStream$
            .pipe(
                buffer(buff$),
            )
            .subscribe((bufferValue: Array<{ node: SgNativeNode; mouseEvent: MouseEvent }>) => {

                const refNode = bufferValue[0].node
                const refMouseEvent = bufferValue[0].mouseEvent

                if (bufferValue.length === 1) {
                    this.nodeClick$.emit({ nodeSchema: refNode.schema, mouseEvent: refMouseEvent })
                    this.selectNode(refNode)
                }
                else if (bufferValue.length > 1) {
                    this.nodeDoubleClick$.emit({ nodeSchema: refNode.schema, mouseEvent: refMouseEvent })
                    if (this._selectedNodeId !== refNode.id) {
                        this.selectNode(refNode)
                    }
                }
            })
    }

    private selectNode(node: SgNativeNode): void {
        // do nothing if selection is disallowed
        if (node.schema?.disallowSelection === true) {
            return
        }

        if (this._selectedNodeId !== node.id) {
            this._selectedNodeId = node.id
            this.nodeSelectionChange$.emit({ nodeSchema: node.schema })
        }
        else {
            this.clearNodeSelection()
        }
    }

    private clearNodeSelection(): void {
        this._selectedNodeId = null
        this.nodeSelectionChange$.emit({ nodeSchema: null })
    }

    private onFocusNode(nodeId): void {

        this._focusedNodeId = nodeId

        // first remove class from currently focused node
        if (this.focusedNodeTimer) {
            clearInterval(this.focusedNodeTimer)
            this.focusedNodeTimer = null
        }

        // remove focus marker after
        const removeClassAfterTimeInMs = 4000

        this.focusedNodeTimer = setInterval(
            () => {
                if (this._focusedNodeId) {
                    this._focusedNodeId = null
                    clearInterval(this.focusedNodeTimer)
                    this.focusedNodeTimer = null
                }
            },
            removeClassAfterTimeInMs,
        )
    }
}
