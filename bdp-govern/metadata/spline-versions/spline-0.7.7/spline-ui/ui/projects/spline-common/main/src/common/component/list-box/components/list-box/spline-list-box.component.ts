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

import { SelectionModel } from '@angular/cdk/collections'
import { Component, EventEmitter, Input, OnChanges, OnInit, Optional, Output, SimpleChanges } from '@angular/core'
import { ControlValueAccessor, NgControl } from '@angular/forms'
import { MatPseudoCheckboxState } from '@angular/material/core'
import { isEqual } from 'lodash-es'
import { takeUntil } from 'rxjs/operators'
import { BaseComponent } from 'spline-utils'

import { SplineListBox } from '../../models'


@Component({
    selector: 'spline-list-box',
    templateUrl: './spline-list-box.component.html',
})
export class SplineListBoxComponent<TValue = unknown> extends BaseComponent
    implements OnChanges, OnInit, ControlValueAccessor {

    @Input() options: SplineListBox.SelectListOption<TValue>[] = []
    @Input() searchTerm = ''
    @Input() trackBy: (value: TValue) => string | number
    @Input() multiple = true
    @Input() selectionModel: SelectionModel<TValue>

    @Output() searchTermChanged$ = new EventEmitter<string>()

    readonly defaultDataMap = SplineListBox.getDefaultDataMap()

    masterSelectState: MatPseudoCheckboxState = 'unchecked'
    isDisabled = false

    _value: TValue[] = []

    private isInitialChange = true

    constructor(@Optional() private readonly ngControl: NgControl) {

        super()

        if (this.ngControl) {
            this.ngControl.valueAccessor = this
        }
    }

    get value(): TValue[] {
        return this._value
    }

    set value(value: TValue[]) {
        if (!isEqual(value, this._value)) {
            this._value = value
            if (this.selectionModel) {
                this.selectionModel.clear()
                this.selectionModel.select(...(this._value || []))
            }
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { records } = changes

        if (records && !records.isFirstChange()) {
            this.masterSelectState = this.calculateMasterSelectState()
        }
    }

    ngOnInit(): void {
        this.selectionModel = new SelectionModel<TValue>(this.multiple)
        this.masterSelectState = this.calculateMasterSelectState()

        this.selectionModel.changed
            .pipe(
                takeUntil(this.destroyed$)
            )
            .subscribe(() => {
                this._value = this.selectionModel.selected
                this.masterSelectState = this.calculateMasterSelectState()

                if (!this.isInitialChange) {
                    this.onChange(this._value)
                }
                this.isInitialChange = false
            })
    }

    trackByFn = (index: number, option: SplineListBox.SelectListOption<TValue>) => {
        return this.trackBy
            ? this.trackBy(option.value)
            : option.value as unknown as string
    }

    onChange = (value: TValue[]): void => {
    }

    onTouched = (): void => {
    }

    registerOnChange(fn: any): void {
        this.onChange = fn
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn
    }

    writeValue(value: TValue[]): void {
        this.value = value
    }

    setDisabledState(isDisabled: boolean): void {
        this.isDisabled = isDisabled
    }

    onSearchTermChanged(searchTerm: string): void {
        this.searchTerm = searchTerm
        this.searchTermChanged$.emit(searchTerm)
    }

    onSelectionOptionClicked(option: SplineListBox.SelectListOption<TValue>): void {
        this.selectionModel.toggle(option.value)
    }

    onMasterSelectClicked(): void {
        if (this.masterSelectState === 'unchecked' || this.masterSelectState === 'indeterminate') {
            this.selectionModel.select(
                ...this.options.map(item => item.value)
            )
        }
        else if (this.masterSelectState === 'checked') {
            this.selectionModel.deselect(
                ...this.options.map(item => item.value)
            )
        }
    }

    private calculateMasterSelectState(): MatPseudoCheckboxState {
        if (this.selectionModel.selected.length === 0) {
            return 'unchecked'
        }

        return this.options.length === this.selectionModel.selected.length
            ? 'checked'
            : 'indeterminate'
    }
}
