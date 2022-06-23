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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core'
import { BehaviorSubject } from 'rxjs'
import { ExecutionPlan } from 'spline-api'
import { SplineDataWidgetEvent } from 'spline-common/data-view'
import { SdWidgetAttributesTree } from 'spline-shared/attributes'
import { BaseLocalStateComponent } from 'spline-utils'

import { ExecutionPlanInfoStore } from '../../store'


@Component({
    selector: 'plan-info',
    templateUrl: './plan-info.component.html',
    styleUrls: ['./plan-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanInfoComponent extends BaseLocalStateComponent<ExecutionPlanInfoStore.State> implements OnChanges {

    @Input() executionPlan: ExecutionPlan
    @Input() set selectedAttributeId(attributeId: string | null) {
        this.selectedAttributeId$.next(attributeId)
    }

    @Output() selectedAttributeChanged$ = new EventEmitter<{ attributeId: string | null }>()

    private readonly selectedAttributeId$ = new BehaviorSubject<string | null>(null)

    constructor() {
        super()
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.executionPlan && !!changes.executionPlan.currentValue) {
            this.updateState(
                ExecutionPlanInfoStore.toState(changes.executionPlan.currentValue, this.selectedAttributeId$),
            )
        }
    }


    onDataViewEvent($event: SplineDataWidgetEvent): void {
        switch ($event.type) {
            // SELECTED ATTR CHANGED
            case SdWidgetAttributesTree.EVENT_TYPE__SELECTED_ATTR_CHANGED:
                this.onSelectedAttributeChanged(($event as SdWidgetAttributesTree.EventSelectedAttrChanged).data.attributeId)
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
