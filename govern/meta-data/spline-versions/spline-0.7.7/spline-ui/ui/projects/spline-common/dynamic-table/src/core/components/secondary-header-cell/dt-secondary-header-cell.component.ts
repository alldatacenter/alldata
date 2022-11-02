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

import { Component, ComponentFactoryResolver, ComponentRef, Input, OnChanges, Type } from '@angular/core'
import { SplineRecord } from 'spline-utils'

import { DynamicTableColumnSchema, IDTSecondaryHeaderCellValueControl } from '../../models'
import { DynamicTableSecondaryHeaderCellManager } from '../../services/dynamic-table-secondary-header-cell.manager'
import { BaseCellComponent } from '../base/base-cell.component'


@Component({
    selector: 'dt-secondary-header-cell',
    template: `
        <ng-container #componentViewContainer></ng-container>
    `,
})
export class DtSecondaryHeaderCellComponent<T, TOption extends SplineRecord = {}>
    extends BaseCellComponent<IDTSecondaryHeaderCellValueControl<T>>
    implements OnChanges {

    @Input() schema: DynamicTableColumnSchema<any, T, any, TOption>;

    constructor(protected componentFactoryResolver: ComponentFactoryResolver,
                protected dynamicTableSecondaryHeaderCellManager: DynamicTableSecondaryHeaderCellManager) {
        super(componentFactoryResolver)
    }

    get componentType(): Type<IDTSecondaryHeaderCellValueControl<T>> {
        return this.dynamicTableSecondaryHeaderCellManager.getComponentType(this.schema.secondaryHeaderType)
    }

    protected initCreatedComponent(componentRef: ComponentRef<IDTSecondaryHeaderCellValueControl<T>>): void {
        // initialize component
        const instance = componentRef.instance
        instance.schema = this.schema

        this.eventsSubscriptionRefs.push(
            instance.event$.subscribe((event) => this.event$.emit(event)),
        )

    }

}
