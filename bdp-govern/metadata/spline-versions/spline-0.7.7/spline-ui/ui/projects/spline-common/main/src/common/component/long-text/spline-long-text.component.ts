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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostListener, Input, ViewChild } from '@angular/core'


@Component({
    selector: 'spline-long-text,[spline-long-text]',
    templateUrl: './spline-long-text.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineLongTextComponent {

    @ViewChild('contentWrapperRef', { read: ElementRef, static: true }) contentWrapperRef: ElementRef<HTMLElement>

    @Input() tooltip: string
    @Input() isTextTooLong = false

    constructor(protected readonly changeDetectionRef: ChangeDetectorRef) {
    }

    @HostListener('mouseover') onHover(): void {
        this.recalculateIsTextTooLong()
    }

    private recalculateIsTextTooLong(): void {
        const isTextTooLong = this.contentWrapperRef.nativeElement.scrollWidth > this.contentWrapperRef.nativeElement.offsetWidth
        if (isTextTooLong !== this.isTextTooLong) {
            this.isTextTooLong = isTextTooLong
            this.changeDetectionRef.detectChanges()
        }
    }

}
