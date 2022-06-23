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

import { Inject, Injectable, Injector, Optional, Type } from '@angular/core'
import { DynamicComponentManager } from 'spline-utils'

import { DF_CONTROL_FACTORY, IDynamicFilterControlComponent, IDynamicFilterControlFactory } from '../models'


@Injectable()
export class DynamicFilterControlManager extends DynamicComponentManager<IDynamicFilterControlFactory, IDynamicFilterControlComponent> {

    constructor(
        @Optional() @Inject(DF_CONTROL_FACTORY) protected factoriesProvidersList: Type<IDynamicFilterControlFactory<any>>[],
        protected injector: Injector
    ) {
        super(injector)
    }

    getComponentType(type: string): Type<IDynamicFilterControlComponent> | null {
        return super.getComponentType(type)
    }

    protected getFactoriesProvidersList(): Type<IDynamicFilterControlFactory<any>>[] {
        return this.factoriesProvidersList
    }


}
