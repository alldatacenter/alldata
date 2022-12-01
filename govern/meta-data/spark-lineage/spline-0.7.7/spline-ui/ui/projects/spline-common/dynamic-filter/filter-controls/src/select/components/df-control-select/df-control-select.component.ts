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

import { ChangeDetectionStrategy, Component, Input, OnInit, ViewChild } from '@angular/core'
import { Observable } from 'rxjs'
import { map, takeUntil } from 'rxjs/operators'
import { SplineListBoxComponent } from 'spline-common'
import { BaseDynamicFilterControlComponent } from 'spline-common/dynamic-filter'

import { DfControlSelect } from '../../models'


@Component({
    selector: 'df-control-select',
    templateUrl: './df-control-select.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DfControlSelectComponent<TId extends keyof any = any>
    extends BaseDynamicFilterControlComponent<DfControlSelect.Value, DfControlSelect.Options, TId>
    implements OnInit {

    @ViewChild(SplineListBoxComponent) splineListBoxComponent: SplineListBoxComponent

    @Input() model: DfControlSelect.Model<TId>

    readonly defaultValueLabelAllSelected = 'COMMON.DF.FILTER_CONTROLS.SELECT.LABEL__ALL_SELECTED'

    stringValues$: Observable<string[]>

    ngOnInit(): void {

        this.stringValues$ = this.model.value$
            .pipe(
                takeUntil(this.destroyed$),
                map(value => {
                    return value?.length
                        ? (value as any[])
                            .map(
                                item => this.model.options.dataMap?.valueToString
                                    ? this.model.options.dataMap.valueToString(item)
                                    : item
                            )
                        : [this.model.options?.valueLabelAllSelected ?? this.defaultValueLabelAllSelected]

                }),
            )
    }

    onApply(): void {
        this.model.patchValue(
            this.getCurrentValue()
        )
    }

    onReset(): void {
        this.model.patchValue(
            []
        )
    }

    private getCurrentValue(): DfControlSelect.Value {
        return this.splineListBoxComponent.value
    }
}
