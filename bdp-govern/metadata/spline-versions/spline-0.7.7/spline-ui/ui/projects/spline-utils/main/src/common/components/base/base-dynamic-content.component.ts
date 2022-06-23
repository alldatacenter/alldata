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

import { Component, ComponentFactoryResolver, ComponentRef, Type, ViewChild, ViewContainerRef } from '@angular/core'
import { Subscription } from 'rxjs'

import { BaseComponent } from './base.component'


@Component({
    selector: 'spline-utils-base-dynamic-content',
    template: ''
})
export abstract class BaseDynamicContentComponent<TComponent> extends BaseComponent {

    @ViewChild('componentViewContainer', { read: ViewContainerRef, static: true })
    componentViewContainerRef: ViewContainerRef

    protected componentRef: ComponentRef<TComponent>
    protected eventsSubscriptionRefs: Subscription[] = []

    protected constructor(protected readonly componentFactoryResolver: ComponentFactoryResolver) {
        super()
    }

    abstract get componentType(): Type<TComponent>;

    protected rebuildComponent(): void {
        this.destroyComponent()
        this.componentRef = this.createComponent()
        this.initCreatedComponent(this.componentRef)
    }

    protected createComponent(): ComponentRef<TComponent> {
        const componentType = this.componentType
        // get component factory
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(componentType)
        // clear view
        this.componentViewContainerRef.clear()
        // create component and attache to the relevant view container
        return this.componentViewContainerRef.createComponent(componentFactory)
    }

    protected destroyComponent(): void {

        if (this.eventsSubscriptionRefs.length > 0) {
            this.eventsSubscriptionRefs
                .forEach(
                    item => item.unsubscribe(),
                )
            this.eventsSubscriptionRefs = []
        }

        if (this.componentRef) {
            this.componentRef.destroy()

        }
    }

    protected abstract initCreatedComponent(componentRef: ComponentRef<TComponent>): void;
}
