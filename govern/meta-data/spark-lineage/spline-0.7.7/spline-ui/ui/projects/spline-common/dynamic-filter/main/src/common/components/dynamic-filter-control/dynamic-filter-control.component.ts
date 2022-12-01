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


import {
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    Type
} from '@angular/core'
import { BaseDynamicContentComponent } from 'spline-utils'

import { DynamicFilterControlManager, IDynamicFilterControlComponent, IDynamicFilterControlModel } from '../../../core'


@Component({
    selector: 'dynamic-filter-control',
    templateUrl: './dynamic-filter-control.component.html'
})
export class DynamicFilterControlComponent
    extends BaseDynamicContentComponent<IDynamicFilterControlComponent> implements OnChanges {

    @Input() model: IDynamicFilterControlModel<any>

    @Output() event$ = new EventEmitter<void>()

    constructor(protected componentFactoryResolver: ComponentFactoryResolver,
                protected dynamicFilterControlManager: DynamicFilterControlManager) {
        super(componentFactoryResolver)
    }

    ngOnChanges(changes: SimpleChanges): void {
        const {model} = changes
        if (model) {
            this.rebuildComponent()
        }
    }

    get componentType(): Type<IDynamicFilterControlComponent> | null {
        return this.dynamicFilterControlManager.getComponentType(this.model.type)
    }

    protected initCreatedComponent(componentRef: ComponentRef<IDynamicFilterControlComponent>): void {
        // initialize component
        const instance = componentRef.instance
        instance.model = this.model

        this.eventsSubscriptionRefs.push(
            instance.event$.subscribe((event) => this.event$.emit(event))
        )
    }
}
