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

import { Directive, forwardRef, Inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core'
import { BehaviorSubject, combineLatest } from 'rxjs'
import { takeUntil } from 'rxjs/operators'
import { BaseDirective } from 'spline-utils'

import { SplineListBoxComponent } from '../components'
import { SplineListBox } from '../models'

@Directive({
    selector: '[splineListBoxRecords]spline-list-box'
})
export class SplineListBoxRecordsDirective<TRecord, TValue> extends BaseDirective implements OnInit, OnChanges, OnDestroy {

    @Input() splineListBoxRecords: TRecord[]

    @Input() dataMap: SplineListBox.DataMap<TRecord, TValue>

    private options$ = new BehaviorSubject<SplineListBox.SelectListOption<TValue>[]>([])
    private search$ = new BehaviorSubject<string>('')

    constructor(@Inject(forwardRef(() => SplineListBoxComponent))
                private readonly splineListBoxComponent: SplineListBoxComponent) {
        super()

    }

    ngOnChanges(changes: SimpleChanges): void {
        const { records } = changes

        if (records && !records.isFirstChange()) {
            this.options$.next(this.calculateSelectOptions(records.currentValue, this.dataMap))
        }
    }

    ngOnInit(): void {

        combineLatest([
            this.options$,
            this.search$,
        ])
            .pipe(
                takeUntil(this.destroyed$)
            )
            .subscribe(([options, searchTerm]) => {
                this.splineListBoxComponent.options = options.filter(item => item.label.toLowerCase().trim().includes(searchTerm))
            })

        this.splineListBoxComponent.searchTermChanged$
            .pipe(
                takeUntil(this.destroyed$)
            )
            .subscribe(
                searchTerm => this.search$.next(searchTerm)
            )

        this.search$.next(this.splineListBoxComponent.searchTerm)
        this.options$.next(this.calculateSelectOptions(this.splineListBoxRecords, this.dataMap))
    }

    ngOnDestroy() {
        super.ngOnDestroy()
        this.options$.complete()
        this.search$.complete()
    }

    private calculateSelectOptions(
        records: TRecord[],
        dataMap: SplineListBox.DataMap<TRecord, TValue>,
    ): SplineListBox.SelectListOption<TValue>[] {
        return records
            .map(
                item => SplineListBox.toListOption(item, dataMap)
            )
    }
}
