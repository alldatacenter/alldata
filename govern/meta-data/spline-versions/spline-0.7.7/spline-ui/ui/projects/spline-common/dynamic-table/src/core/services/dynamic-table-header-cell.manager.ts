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

import { Inject, Injectable, InjectionToken, Injector, Optional, Type } from '@angular/core'
import { DynamicComponentManager } from 'spline-utils'

import { DtHeaderCellDefaultComponent } from '../components/header-cell/type'
import { IDtHeaderCellFactory, IDTHeaderCellValueControl } from '../models'


export const DT_HEADER_CELL_FACTORY = new InjectionToken<IDtHeaderCellFactory<any>[]>('DT_HEADER_CELL_FACTORY')

@Injectable()
export class DynamicTableHeaderCellManager extends DynamicComponentManager<IDtHeaderCellFactory<any>, IDTHeaderCellValueControl<any>> {

    constructor(
        @Optional() @Inject(DT_HEADER_CELL_FACTORY) protected factoriesProvidersList: Type<IDtHeaderCellFactory<any>>[],
        protected injector: Injector,
    ) {
        super(injector)
    }

    getComponentType(cellType?: string): Type<IDTHeaderCellValueControl<any>> {
        // in case if tye is not specified return the default one.
        if (!cellType) {
            return DtHeaderCellDefaultComponent
        }

        return super.getComponentType(cellType)
    }

    protected getFactoriesProvidersList(): Type<IDtHeaderCellFactory<any>>[] {
        return this.factoriesProvidersList
    }


}
