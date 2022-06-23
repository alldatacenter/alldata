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

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core'
import { MatMenuTrigger } from '@angular/material/menu'
import moment from 'moment'
import { DaterangepickerComponent } from 'ngx-daterangepicker-material'
import { BaseLocalStateComponent } from 'spline-utils'

import { SplineDateRangeFilter } from './spline-date-range-filter.models'


@Component({
    selector: 'spline-date-filter',
    templateUrl: './spline-date-range-filter.component.html',
})
export class SplineDateRangeFilterComponent extends BaseLocalStateComponent<SplineDateRangeFilter.State> implements OnChanges {

    // TODO: labels localization is needed
    readonly customDateRanges = {
        Today: [moment().startOf('day'), moment().endOf('day')],
        Yesterday: [moment().subtract(1, 'day').startOf('day'), moment().subtract(1, 'day').endOf('day')],
        'This Week': [moment().startOf('isoWeek'), moment().endOf('isoWeek')],
        'Last Week': [moment().subtract(1, 'week').startOf('isoWeek'), moment().subtract(1, 'week').endOf('isoWeek')],
        'This Month': [moment().startOf('month'), moment().endOf('month')],
        'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
        'Last 3 Month': [moment().subtract(3, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
    }

    @ViewChild(DaterangepickerComponent, { static: false }) datePicker: DaterangepickerComponent
    @ViewChild(MatMenuTrigger, { static: false }) matMenuTrigger: MatMenuTrigger

    @Input() value: SplineDateRangeFilter.Value | null
    @Input() showIcon = true

    @Input() icon = 'schedule'
    @Input() label: string

    @Input() emptyValueString = 'COMMON.DATE_FILTER__EMPTY_VALUE_LABEL'

    @Input() set maxDate(value: Date) {
        this._maxDateMoment = value ? moment(value).endOf('day') : null
    }

    @Input() set minDate(value: Date) {
        this._minDateMoment = value ? moment(value).startOf('day') : null
    }

    @Output() valueChanged$ = new EventEmitter<SplineDateRangeFilter.Value>()

    _maxDateMoment: moment.Moment
    _minDateMoment: moment.Moment

    readonly _defaultValue = moment()


    constructor() {
        super()
        this.updateState(
            SplineDateRangeFilter.getDefaultState()
        )
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { value } = changes
        if (value) {
            this.setValue(value.currentValue)
        }
    }

    onDateChosen($event: { chosenLabel: string; startDate: moment.Moment; endDate: moment.Moment }): void {
        const newValue = $event.startDate && $event.endDate
            ? {
                dateFrom: $event.startDate.startOf('day').toDate(),
                dateTo: $event.endDate.endOf('day').toDate()
            }
            : null
        this.setValue(newValue)
        this.valueChanged$.emit(newValue)
    }

    onCloseDatePicker(): void {
        this.datePicker.updateView()
        this.matMenuTrigger.closeMenu()
    }

    private setValue(value: SplineDateRangeFilter.Value): void {
        this.updateState(
            SplineDateRangeFilter.reduceValueChanged(this.state, value)
        )
    }
}
