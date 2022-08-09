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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core'


@Component({
    selector: 'spline-content-error',
    templateUrl: './spline-content-error.component.html',
})
export class SplineContentErrorComponent implements OnChanges {

    @Input() floating = true
    @Input() errorId: string
    @Input() statusCode: 500 | 404 | 403 | number

    readonly defaultErrorMessage = 'COMMON.UNEXPECTED_ERROR__MESSAGE'

    errorMessage = this.defaultErrorMessage

    ngOnChanges(changes: SimpleChanges): void {
        const { statusCode } = changes
        if (statusCode) {
            this.errorMessage = this.calculateErrorMessage(statusCode.currentValue)
        }
    }

    private calculateErrorMessage(statusCode: number): string {

        if (statusCode === 0) {
            return 'COMMON.SERVER_COMMUNICATION_ERROR__MESSAGE__FAILURE'
        }
        else if (statusCode === 404) {
            return 'COMMON.SERVER_COMMUNICATION_ERROR__MESSAGE__NOT_FOUND'
        }
        else if (statusCode >= 500 && statusCode < 600) {
            return 'COMMON.SERVER_COMMUNICATION_ERROR__MESSAGE'
        }
        else {
            return this.defaultErrorMessage
        }

    }
}
