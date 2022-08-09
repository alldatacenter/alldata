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

import { DynamicTableColumnSchema, IDTHeaderCellValueControl } from '../../models'
import { DynamicTableHeaderCellManager } from '../../services/dynamic-table-header-cell.manager'
import { BaseCellComponent } from '../base/base-cell.component'


@Component({
    selector: 'dt-header-cell',
    templateUrl: './dt-header-cell.component.html'
})
export class DtHeaderCellComponent<T, TOption extends SplineRecord = {}> extends BaseCellComponent<IDTHeaderCellValueControl<T>>
    implements OnChanges {

    @Input() schema: DynamicTableColumnSchema<any, T, any, TOption>;

    constructor(protected componentFactoryResolver: ComponentFactoryResolver,
                protected dynamicTableHeaderCellManager: DynamicTableHeaderCellManager) {
        super(componentFactoryResolver)
    }

    get componentType(): Type<IDTHeaderCellValueControl<T>> {
        return this.dynamicTableHeaderCellManager.getComponentType(this.schema.headerType)
    }

    protected initCreatedComponent(componentRef: ComponentRef<IDTHeaderCellValueControl<T>>): void {
        // initialize component
        const instance = componentRef.instance
        instance.schema = this.schema

        this.eventsSubscriptionRefs.push(
            instance.event$.subscribe((event) => this.event$.emit(event)),
        )
    }

}
