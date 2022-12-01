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

import {
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    Type,
} from '@angular/core'
import { BaseDynamicContentComponent } from 'spline-utils'

import { ISplineDataWidget, SdWidgetSchema, SplineDataWidgetEvent } from '../../models'
import { SplineDataWidgetManager } from '../../services'


@Component({
    selector: 'spline-data-widget',
    templateUrl: './spline-data-widget.component.html',
})
export class SplineDataWidgetComponent<TData extends object, TOptions extends object = {}>
    extends BaseDynamicContentComponent<ISplineDataWidget<TData, TOptions>> implements OnChanges {

    @Input() schema: SdWidgetSchema<TData, TOptions>
    @Output() event$ = new EventEmitter<SplineDataWidgetEvent>()

    constructor(protected readonly componentFactoryResolver: ComponentFactoryResolver,
                private readonly splineDataWidgetManager: SplineDataWidgetManager) {

        super(componentFactoryResolver)

    }

    ngOnChanges(changes: SimpleChanges): void {
        const schemaChange = changes['schema']
        if (schemaChange) {
            this.rebuildComponent()
        }
    }

    protected initCreatedComponent(componentRef: ComponentRef<ISplineDataWidget<TData>>): void {
        // initialize component
        const instance = componentRef.instance
        instance.schema = this.schema

        this.eventsSubscriptionRefs.push(
            instance.event$.subscribe((event) => this.event$.emit(event)),
        )
    }

    get componentType(): Type<ISplineDataWidget<TData, TOptions>> {
        return this.splineDataWidgetManager.getComponentType(this.schema.type)
    }
}
