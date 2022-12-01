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

import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core'
import { BehaviorSubject } from 'rxjs'
import { filter, takeUntil } from 'rxjs/operators'
import { ExecutionPlanAgentInfo, ExecutionPlanFacade, ExecutionPlanLineageNode } from 'spline-api'
import { SplineDataWidgetEvent } from 'spline-common/data-view'
import { SdWidgetAttributesTree } from 'spline-shared/attributes'
import { SgNodeCardDataView } from 'spline-shared/data-view'
import NodeEventData = SgNodeCardDataView.NodeEventData
import { BaseLocalStateComponent, GenericEventInfo } from 'spline-utils'

import { OperationDetailsDataSource } from '../../data-sources'
import { OperationInfo } from '../../models'


@Component({
    selector: 'plan-operation-info',
    templateUrl: './operation-info.component.html',
    styleUrls: ['./operation-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: OperationDetailsDataSource,
            useFactory: (executionPlanFacade: ExecutionPlanFacade) => {
                return new OperationDetailsDataSource(executionPlanFacade)
            },
            deps: [ExecutionPlanFacade],
        },
    ],
})
export class OperationInfoComponent extends BaseLocalStateComponent<OperationInfo.State> implements OnChanges {

    @Input() node: ExecutionPlanLineageNode

    @Input() agentInfo: ExecutionPlanAgentInfo | undefined

    @Input() set selectedAttributeId(attributeId: string | null) {
        this.selectedAttributeId$.next(attributeId)
    }

    @Output() selectedAttributeChanged$ = new EventEmitter<{ attributeId: string | null }>()
    @Output() focusNode$ = new EventEmitter<{ nodeId: string }>()

    private readonly selectedAttributeId$ = new BehaviorSubject<string | null>(null)

    constructor(readonly dataSource: OperationDetailsDataSource) {
        super()

        this.dataSource.data$
            .pipe(
                filter(state => !!state),
                takeUntil(this.destroyed$),
            )
            .subscribe(data =>
                this.updateState({
                    operationDvs: OperationInfo.toDataViewSchema(data.operation),
                    inputsDvs: OperationInfo.toInputsDvs(data, this.selectedAttributeId$),
                    outputDvs: OperationInfo.toOutputsDvs(data, this.selectedAttributeId$),
                    detailsDvs: OperationInfo.toDetailsDvs(data, this.agentInfo),
                    inputsNumber: data?.inputs?.length ?? 0
                }),
            )
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.node && !!changes.node.currentValue) {
            this.dataSource.setFilter({
                operationId: changes.node.currentValue.id,
            })
        }
    }

    onDataViewEvent($event: SplineDataWidgetEvent): void {
        switch ($event.type) {
            // SELECTED ATTR CHANGED
            case SdWidgetAttributesTree.EVENT_TYPE__SELECTED_ATTR_CHANGED:

                this.onSelectedAttributeChanged(($event as SdWidgetAttributesTree.EventSelectedAttrChanged).data.attributeId)

                break

            case SgNodeCardDataView.WidgetEvent.FocusNode:
                this.focusNode$.next({ nodeId: ($event as GenericEventInfo<NodeEventData>).data.nodeId })
                break

            default:
            // DO NOTHING
        }
    }

    private onSelectedAttributeChanged(attributeId: string | null): void {
        this.selectedAttributeId$.next(attributeId)
        this.selectedAttributeChanged$.emit({ attributeId })
    }

}
