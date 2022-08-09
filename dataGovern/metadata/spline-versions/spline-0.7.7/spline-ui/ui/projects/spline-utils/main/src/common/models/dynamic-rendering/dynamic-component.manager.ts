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

import { Injector, Type } from '@angular/core'


export interface IDynamicComponentFactory<TComponent = any> {
    readonly type: string
    readonly componentType: Type<TComponent>
}

export abstract class DynamicComponentManager<TFactory extends IDynamicComponentFactory, TComponent> {

    // manually registration
    protected staticFactoriesMap = new Map<string, TFactory>()

    protected constructor(protected readonly injector: Injector) {
    }

    getFactory(type: string): TFactory | undefined {

        const factoriesMapFromProviders = this.toFactoriesMap(this.getFactoriesProvidersList() || [])

        if (factoriesMapFromProviders.has(type)) {
            return factoriesMapFromProviders.get(type)
        }

        if (this.staticFactoriesMap.has(type)) {
            return this.staticFactoriesMap.get(type)
        }


        return undefined
    }

    getComponentType(type: string): Type<TComponent> | null {
        const factory = this.getFactory(type)
        return factory
            ? factory.componentType
            : null
    }

    registerStaticFactory(factory: TFactory): void {
        this.staticFactoriesMap.set(factory.type, factory)
    }

    protected toFactoriesMap(factoriesListProvider: Type<TFactory>[]): Map<string, TFactory> {
        if (factoriesListProvider === null) {
            throw new Error('No factory provider found')
        }
        return factoriesListProvider
            .reduce((map, currentFactory) => {
                const factoryInstance = this.injector.get<any>(currentFactory)
                return map.set(factoryInstance.type, factoryInstance)
            }, new Map<string, TFactory>())
    }

    protected abstract getFactoriesProvidersList(): Type<TFactory>[]

}
