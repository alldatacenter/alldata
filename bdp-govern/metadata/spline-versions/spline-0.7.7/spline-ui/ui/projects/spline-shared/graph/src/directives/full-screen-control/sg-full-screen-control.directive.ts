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


import { DOCUMENT } from '@angular/common'
import {
    Directive,
    EventEmitter,
    forwardRef,
    HostListener,
    Inject,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core'

import { SgContainerComponent } from '../../components'


@Directive({
    selector: '[sgFullScreenControl]'
})
export class SgFullScreenControlDirective implements OnDestroy, OnInit, OnChanges {

    @Input() isFullScreenMode: boolean
    @Output() isFullScreenModeChange = new EventEmitter<boolean>()

    readonly fullScreenBodyClassName = 'body-with-full-screen-sg-container'

    private _isFullScreenMode = false

    constructor(@Inject(forwardRef(() => SgContainerComponent)) private readonly sgContainerComponent: SgContainerComponent,
                @Inject(DOCUMENT) private readonly document: Document) {
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { isFullScreenMode } = changes
        if (isFullScreenMode && !isFullScreenMode.isFirstChange() && this._isFullScreenMode) {
            this.applyFullScreenMode(isFullScreenMode.currentValue)
        }
    }

    ngOnInit(): void {
        this.applyFullScreenMode(this.isFullScreenMode)
    }

    ngOnDestroy(): void {
        this.applyFullScreenMode(false)
    }

    @HostListener('click', ['$event'])
    onClick($event: MouseEvent): void {
        $event.stopPropagation()
        $event.preventDefault()

        this.toggleFullScreenMode()
    }

    @HostListener('window:keyup', ['$event'])
    onEscapeBtnClicked($event: KeyboardEvent): void {
        if ($event.code === 'Escape' && this.isFullScreenMode) {
            this.toggleFullScreenMode()
        }
    }

    private toggleFullScreenMode(): void {
        this.isFullScreenMode = !this.isFullScreenMode
        this.isFullScreenModeChange.emit(this.isFullScreenMode)
        this.applyFullScreenMode(this.isFullScreenMode)
    }

    private applyFullScreenMode(isFullScreen: boolean): void {
        if (isFullScreen) {
            this.document.body.classList.add(this.fullScreenBodyClassName)
        }
        else {
            this.document.body.classList.remove(this.fullScreenBodyClassName)
        }

        if (this.sgContainerComponent) {
            this.sgContainerComponent.refresh()
        }
    }

}
