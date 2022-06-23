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


import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core'
import { MatMenuTrigger } from '@angular/material/menu'

import { SplineInlineFilterWithOverlay } from './spline-inline-filter-with-overlay.models'


@Component({
    selector: 'spline-inline-filter-with-overlay',
    templateUrl: './spline-inline-filter-with-overlay.component.html'
})
export class SplineInlineFilterWithOverlayComponent {

    @Input() icon: string
    @Input() label: string
    @Input() set filterStringValues(value: string[] | string) {
        this._filterStringValues = Array.isArray(value) ? value : [value]
    }

    @Input() showActions = true
    @Input() applyBtnDisabled = false
    @Input() options: SplineInlineFilterWithOverlay.Options = SplineInlineFilterWithOverlay.getDefaultOptions()

    @Output() apply$ = new EventEmitter<void>()
    @Output() cancel$ = new EventEmitter<void>()
    @Output() reset$ = new EventEmitter<void>()
    @Output() panelClosed$ = new EventEmitter<void>()
    @Output() panelOpened$ = new EventEmitter<void>()

    @ViewChild(MatMenuTrigger, { static: false }) matMenuTrigger: MatMenuTrigger

    _filterStringValues: string[] = []

    get isOpened(): boolean {
        return this.matMenuTrigger?.menuOpen ?? false
    }

    onClearBtnClicked(): void {
        this.reset$.emit()
        this.closePanel()
    }

    onCancelBtnClicked($event: MouseEvent): void {
        this.cancel$.emit()
        this.closePanel()
    }

    onApplyBtnClicked($event: MouseEvent): void {
        this.apply$.emit()
        this.closePanel()
    }

    private closePanel(): void {
        this.matMenuTrigger.closeMenu()
    }

}
