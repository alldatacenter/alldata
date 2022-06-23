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


import { ChangeDetectorRef, Directive, ElementRef, Input, OnChanges, OnDestroy, Optional, Renderer2, SimpleChanges } from '@angular/core'
import { NavigationEnd, Router, RouterLink, RouterLinkWithHref } from '@angular/router'
import { filter, takeUntil } from 'rxjs/operators'

import { BaseDirective } from '../base'

import { RouterLinkActivePattern } from './router-link-active-pattern.models'


@Directive({
    selector: '[splineRouterLinkActivePattern]'
})
export class RouterLinkActivePatternDirective extends BaseDirective implements OnChanges, OnDestroy {

    @Input() splineRouterLinkActivePattern: string

    private isActive = false
    private cssClassesList: string[] = ['active']
    private readonly exactRouteComparison = true

    constructor(private router: Router,
                private element: ElementRef,
                private renderer: Renderer2,
                private readonly changeDetectorRef: ChangeDetectorRef,
                @Optional() private routerLink?: RouterLink,
                @Optional() private routerLinkWithHref?: RouterLinkWithHref) {
        super()

        this.router.events
            .pipe(
                takeUntil(this.destroyed$),
                filter(event => event instanceof NavigationEnd)
            )
            .subscribe((event: NavigationEnd) => {
                this.onUrlChanged()
            })
    }

    @Input() set splineRouterLinkActiveClasses(data: string[] | string) {
        const classes = Array.isArray(data) ? data : data.split(' ')
        this.cssClassesList = classes.filter(c => !!c)
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.updateActiveState()
    }

    private onUrlChanged(): void {
        this.updateActiveState()
    }

    private isCurrentLinkActive(): boolean {
        if (this.splineRouterLinkActivePattern) {
            const currentUrl = this.router.getCurrentNavigation()
                ? this.router.getCurrentNavigation()?.finalUrl?.toString()
                : this.router.url

            return RouterLinkActivePattern.isUrlActive(currentUrl, this.splineRouterLinkActivePattern)
        }
        else if (this.routerLink) {
            return this.router.isActive(this.routerLink.urlTree, this.exactRouteComparison)
        }
        else if (this.routerLinkWithHref) {
            return this.router.isActive(this.routerLink.urlTree, this.exactRouteComparison)
        }

        return false
    }

    private updateActiveState(): void {
        if (!this.router.navigated) {
            return
        }

        const isActive = this.isCurrentLinkActive()

        if (this.isActive !== isActive) {
            this.isActive = isActive
            this.changeDetectorRef.markForCheck()

            this.cssClassesList.forEach((c) => {
                if (isActive) {
                    this.renderer.addClass(this.element.nativeElement, c)
                }
                else {
                    this.renderer.removeClass(this.element.nativeElement, c)
                }
            })
        }
    }
}
