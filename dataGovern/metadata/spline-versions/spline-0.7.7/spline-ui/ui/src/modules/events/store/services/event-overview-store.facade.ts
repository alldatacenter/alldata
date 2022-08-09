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

import { Injectable } from '@angular/core'
import { Observable, of } from 'rxjs'
import { catchError, take, tap } from 'rxjs/operators'
import { ExecutionEventFacade, ExecutionEventLineageNode, ExecutionEventLineageOverview } from 'spline-api'
import { SgNodeControl } from 'spline-shared/graph'
import { BaseStore, ProcessingStore } from 'spline-utils'

import { EventOverviewStore } from '../models'


@Injectable()
export class EventOverviewStoreFacade extends BaseStore<EventOverviewStore.State> {

    constructor(private readonly executionEventFacade: ExecutionEventFacade) {
        super(EventOverviewStore.getDefaultState())
    }

    setSelectedNode(nodeId: string | null): void {
        if (this.state.selectedNodeId !== nodeId) {
            this.updateState(
                EventOverviewStore.reduceSelectedNodeId(nodeId, this.state)
            )
        }
    }

    setGraphNodeView(graphNodeView: SgNodeControl.NodeView): void {
        this.updateState(
            EventOverviewStore.reduceGraphNodeView(this.state, graphNodeView)
        )
    }

    setGraphDepth(graphDepth: number): void {
        this.loadData(
            this.state.executionEventId,
            graphDepth,
            state => ({
                ...state,
                graphLoadingProcessing: ProcessingStore.eventProcessingStart(this.state.graphLoadingProcessing)
            }),
            state => ({
                ...state,
                graphLoadingProcessing: ProcessingStore.eventProcessingFinish(this.state.graphLoadingProcessing)
            }),
            (state, error) => ({
                ...state,
                graphLoadingProcessing: ProcessingStore.eventProcessingFinish(this.state.graphLoadingProcessing, error)
            })
        )
            .subscribe()
    }

    init(
        executionEventId: string,
        graphDepth: number,
    ): void {

        this.loadData(
            executionEventId,
            graphDepth,
            state => ({
                ...state,
                loadingProcessing: ProcessingStore.eventProcessingStart(this.state.loadingProcessing)
            }),
            state => ({
                ...state,
                loadingProcessing: ProcessingStore.eventProcessingFinish(this.state.loadingProcessing),
            }),
            (state, error) => ({
                ...state,
                loadingProcessing: ProcessingStore.eventProcessingFinish(this.state.loadingProcessing, error),
            }),
        )
            .subscribe(() => {
                this.updateState({
                    executionEventId,
                })
            })
    }

    findNode(nodeId: string): ExecutionEventLineageNode {
        return EventOverviewStore.selectNode(this.state, nodeId)
    }

    findChildrenNodes(nodeId: string): ExecutionEventLineageNode[] {
        return EventOverviewStore.selectChildrenNodes(this.state, nodeId)
    }

    findParentNodes(nodeId: string): ExecutionEventLineageNode[] {
        return EventOverviewStore.selectParentNodes(this.state, nodeId)
    }

    // TODO: remove it after BE will support it.
    loadNodeHistory(nodeId: string): void {

        this.updateState({
            graphLoadingProcessing: ProcessingStore.eventProcessingStart(this.state.graphLoadingProcessing)
        })

        setTimeout(() => {
            this.updateState({
                ...EventOverviewStore.__reduceFakeHistoryNode(this.state, nodeId),
                graphLoadingProcessing: ProcessingStore.eventProcessingFinish(this.state.graphLoadingProcessing)
            })
        }, 500)
    }

    // TODO: remove it after BE will support it.
    loadNodeFuture(nodeId: string): void {

        this.updateState({
            graphLoadingProcessing: ProcessingStore.eventProcessingStart(this.state.graphLoadingProcessing)
        })

        setTimeout(() => {
            this.updateState({
                ...EventOverviewStore.__reduceFakeFutureNode(this.state, nodeId),
                graphLoadingProcessing: ProcessingStore.eventProcessingFinish(this.state.graphLoadingProcessing)
            })
        }, 500)
    }

    private loadData(
        executionEventId: string,
        graphDepth: number,
        onLoadingStart: (state: EventOverviewStore.State) => EventOverviewStore.State,
        onLoadingSuccess: (state: EventOverviewStore.State) => EventOverviewStore.State,
        onLoadingError: (state: EventOverviewStore.State, error: any | null) => EventOverviewStore.State,
    ): Observable<ExecutionEventLineageOverview> {

        this.updateState({
            ...onLoadingStart(this.state)
        })

        return this.executionEventFacade.fetchLineageOverview(executionEventId, graphDepth)
            .pipe(
                catchError((error) => {
                    this.updateState({
                        ...onLoadingError(this.state, error)
                    })
                    return of(null)
                }),
                // update data state
                tap((lineageData) => {
                    if (lineageData !== null) {
                        this.updateState({
                            ...EventOverviewStore.reduceLineageOverviewData(
                                onLoadingSuccess(this.state),
                                executionEventId,
                                lineageData
                            ),
                        })
                    }
                }),
                take(1),
            )
    }

}
