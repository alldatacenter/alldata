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

import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling'
import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ContentChild,
    ElementRef,
    HostListener,
    OnDestroy,
    Renderer2,
    ViewChild
} from '@angular/core'
import { BaseComponent, DomHelpers } from 'spline-utils'


@Component({
    selector: 'spline-virtual-scroll-sticky-container',
    templateUrl: './spline-virtual-scroll-sticky-container.component.html',
})
export class SplineVirtualScrollStickyContainerComponent extends BaseComponent implements AfterViewInit, OnDestroy {

    @ViewChild('scrollContainerRef', { static: true, read: ElementRef }) scrollContainerRef: ElementRef<HTMLElement>
    @ViewChild('scrollSpacerRef', { static: true, read: ElementRef }) scrollSpacerRef: ElementRef<HTMLElement>

    @ContentChild(CdkVirtualScrollViewport) viewPort: CdkVirtualScrollViewport

    private scrollListenerFn: () => void

    constructor(private readonly elementRef: ElementRef,
                private readonly renderer2: Renderer2,
                private readonly changeDetectorRef: ChangeDetectorRef) {

        super()

    }

    @HostListener('window:resize', ['$event'])
    onElementScroll(): void {
        this.updateContainerSizes()
    }

    ngAfterViewInit(): void {

        this.watchAndSyncScrollingStart()
        setTimeout(() => this.updateContainerSizes(), 200)

    }

    ngOnDestroy(): void {
        super.ngOnDestroy()
        this.watchAndSyncScrollingStop()
    }

    checkContainerSize(): void {
        this.updateContainerSizes()
    }

    private watchAndSyncScrollingStart(): void {
        const parentScrollElement = this.getNearestParentWithScroll()
        if (parentScrollElement) {
            this.scrollListenerFn = this.renderer2.listen(
                parentScrollElement,
                'scroll',
                (e: Event) => {
                    this.updateScrollState()
                }
            )
        }
    }

    private watchAndSyncScrollingStop(): void {
        this.scrollListenerFn()
    }

    private updateContainerSizes(): void {
        const domRelaxationTime = 100
        setTimeout(
            () => {
                const scrollContainerHeight = this.scrollContainerRef.nativeElement.clientHeight
                // get height of the virtual scroll spacer
                const originalSpacerHeight = this.viewPort.getElementRef().nativeElement
                    .querySelector('.cdk-virtual-scroll-spacer').clientHeight

                // calculate & set our custom spacer height
                const spacerHeight = originalSpacerHeight - scrollContainerHeight
                this.renderer2.setStyle(this.scrollSpacerRef.nativeElement, 'height', `${spacerHeight > 0 ? spacerHeight : 0}px`)

                // update view port size & synchronize scroll position
                setTimeout(
                    () => {
                        this.renderer2.setStyle(this.viewPort.getElementRef().nativeElement, 'height', `${scrollContainerHeight}px`)
                        this.updateScrollState()
                    },
                    domRelaxationTime
                )
            },
            domRelaxationTime
        )
    }

    private updateScrollState(): void {
        // get nearest parent with a scroll
        const targetElement = this.getNearestParentWithScroll()
        // calculate scrollOffset for the sticky view port
        const scrollOffset = targetElement.scrollTop >= this.elementRef.nativeElement.offsetTop
            ? targetElement.scrollTop - this.elementRef.nativeElement.offsetTop
            : 0
        // scroll to the relevant offset & force change detection
        this.viewPort.scrollToOffset(scrollOffset)
        this.viewPort.checkViewportSize()
        this.changeDetectorRef.detectChanges()
    }

    private getNearestParentWithScroll(): HTMLElement {
        return DomHelpers.getScrollParent(this.elementRef.nativeElement)
    }


}
