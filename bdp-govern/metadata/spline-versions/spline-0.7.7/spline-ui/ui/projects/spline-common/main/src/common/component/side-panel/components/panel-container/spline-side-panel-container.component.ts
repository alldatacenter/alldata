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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core'


@Component({
    selector: 'spline-side-dialog-container',
    templateUrl: './spline-side-panel-container.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineSidePanelContainerComponent implements OnInit {

    @Input() set visible(isVisible: boolean) {
        if (isVisible !== this._isVisible) {
            this.processVisibilityChange(isVisible)
        }
    }

    @Input() zIndex = 200

    @Output() closed$ = new EventEmitter<void>()
    @Output() opened$ = new EventEmitter<void>()

    _isVisible = false

    constructor(public changeDetectorRef: ChangeDetectorRef) {
    }

    ngOnInit(): void {
    }

    onCloseButtonClicked(): void {
        this.hide()
    }

    show(): void {
        this.processVisibilityChange(true)
    }

    hide(): void {
        this.processVisibilityChange(false)
    }

    private processVisibilityChange(isVisible: boolean): void {
        if (isVisible !== this._isVisible) {
            this._isVisible = isVisible
            this.changeDetectorRef.detectChanges()

            if (this._isVisible) {
                this.opened$.emit()
            }
            else {
                setTimeout(() => {
                    this.closed$.emit()
                })
            }
        }
    }

}
