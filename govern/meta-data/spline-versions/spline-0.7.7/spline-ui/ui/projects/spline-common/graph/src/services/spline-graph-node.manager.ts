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

import {
    SgNodeCircle,
    SgNodeCircleButton,
    SgNodeCircleButtonComponent,
    SgNodeCircleComponent,
    SgNodeDefaultComponent
} from '../components/node-control/type'
import { ISgNodeControl, ISgNodeControlFactory } from '../models'


export const SG_NODE_CONTROL_FACTORY = new InjectionToken<ISgNodeControlFactory<any>[]>('SG_NODE_CONTROL_FACTORY')


@Injectable()
export class SplineGraphNodeManager extends DynamicComponentManager<ISgNodeControlFactory<any>, ISgNodeControl<any>> {

    readonly defaultNodeControlsMap: { [type: string]: Type<ISgNodeControl<any>> } = {
        [SgNodeCircle.TYPE]: SgNodeCircleComponent,
        [SgNodeCircleButton.TYPE]: SgNodeCircleButtonComponent
    }

    constructor(
        @Optional() @Inject(SG_NODE_CONTROL_FACTORY) protected readonly factoriesProvidersList: Type<ISgNodeControlFactory<any>>[],
        protected readonly injector: Injector,
    ) {
        super(injector)
    }

    getComponentType(type?: string): Type<ISgNodeControl<any, any>> | null {
        // in case if type is not specified return the default one.
        if (!type) {
            return SgNodeDefaultComponent
        }

        // default types
        if (this.defaultNodeControlsMap[type]) {
            return this.defaultNodeControlsMap[type]
        }

        return super.getComponentType(type)
    }

    protected getFactoriesProvidersList(): Type<ISgNodeControlFactory<any>>[] {
        return this.factoriesProvidersList
    }


}
