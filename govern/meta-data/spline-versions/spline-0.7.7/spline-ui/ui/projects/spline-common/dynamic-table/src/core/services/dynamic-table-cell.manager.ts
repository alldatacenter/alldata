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

import { DtCellDefaultComponent } from '../components/cell/type'
import { IDtCellFactory, IDTCellValueControl } from '../models'


export const DT_CELL_FACTORY = new InjectionToken<IDtCellFactory<any>[]>('DT_CELL_FACTORY')


@Injectable()
export class DynamicTableCellManager extends DynamicComponentManager<IDtCellFactory<any>, IDTCellValueControl<any>> {
    constructor(
        @Optional() @Inject(DT_CELL_FACTORY) protected factoriesProvidersList: Type<IDtCellFactory<any>>[],
        protected injector: Injector
    ) {
        super(injector)
    }

    getComponentType(cellType?: string): Type<IDTCellValueControl<any>> | null {
        // in case if type is not specified return the default one.
        if (!cellType) {
            return DtCellDefaultComponent
        }

        return super.getComponentType(cellType)
    }

    protected getFactoriesProvidersList(): Type<IDtCellFactory<any>>[] {
        return this.factoriesProvidersList
    }


}
