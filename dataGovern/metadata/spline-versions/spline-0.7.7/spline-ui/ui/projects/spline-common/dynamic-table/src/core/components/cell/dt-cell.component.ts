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

import { Component, ComponentFactoryResolver, ComponentRef, Input, OnChanges, OnDestroy, Type } from '@angular/core'
import { SplineRecord } from 'spline-utils'

import { IDTCellValueControl } from '../../models/cell-value-control.models'
import { DynamicTableColumnSchema } from '../../models/dynamic-table.models'
import { DynamicTableCellManager } from '../../services/dynamic-table-cell.manager'
import { BaseCellComponent } from '../base/base-cell.component'


@Component({
    selector: 'dt-cell',
    templateUrl: './dt-cell.component.html',
})
export class DtCellComponent<T, TOptions extends SplineRecord = {}>
    extends BaseCellComponent<IDTCellValueControl<T>> implements OnDestroy, OnChanges {

    @Input() schema: DynamicTableColumnSchema<T, TOptions>;
    @Input() rowData: any;

    constructor(protected componentFactoryResolver: ComponentFactoryResolver,
                protected dynamicTableCellManager: DynamicTableCellManager) {
        super(componentFactoryResolver)
    }

    get componentType(): Type<IDTCellValueControl<T>> {
        return this.dynamicTableCellManager.getComponentType(this.schema.type)
    }

    protected initCreatedComponent(componentRef: ComponentRef<IDTCellValueControl<T>>): void {
        // initialize component
        const instance = componentRef.instance
        instance.schema = this.schema
        instance.rowData = this.rowData

        this.eventsSubscriptionRefs.push(
            instance.event$.subscribe((event) => this.event$.emit(event))
        )
    }

}
