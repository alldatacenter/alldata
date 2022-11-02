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

import { DtSecondaryHeaderCellDefaultComponent } from '../components/secondary-header-cell/type'
import { IDtSecondaryHeaderCellFactory, IDTSecondaryHeaderCellValueControl } from '../models'


export const DT_SECONDARY_HEADER_CELL_FACTORY =
    new InjectionToken<IDtSecondaryHeaderCellFactory<any>[]>('DT_SECONDARY_HEADER_CELL_FACTORY')

@Injectable()
export class DynamicTableSecondaryHeaderCellManager
    extends DynamicComponentManager<IDtSecondaryHeaderCellFactory<any>, IDTSecondaryHeaderCellValueControl<any>> {

    constructor(
        @Optional() @Inject(DT_SECONDARY_HEADER_CELL_FACTORY) protected factoriesProvidersList: Type<IDtSecondaryHeaderCellFactory<any>>[],
        protected injector: Injector,
    ) {
        super(injector)
    }

    getComponentType(cellType?: string): Type<IDTSecondaryHeaderCellValueControl<any>> {
        // in case if tye is not specified return the default one.
        if (!cellType) {
            return DtSecondaryHeaderCellDefaultComponent
        }

        return super.getComponentType(cellType)
    }

    protected getFactoriesProvidersList(): Type<IDtSecondaryHeaderCellFactory<any>>[] {
        return this.factoriesProvidersList
    }


}
