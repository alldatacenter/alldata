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

import { Injectable } from '@angular/core'
import { forkJoin, Observable, of } from 'rxjs'
import { map } from 'rxjs/operators'
import { SplineRecord } from 'spline-utils'

import { DynamicFilterControlSchema, DynamicFilterModel, DynamicFilterSchema, IDynamicFilterControlModel } from '../models'

import { DynamicFilterControlManager } from './dynamic-filter-control.manager'


@Injectable()
export class DynamicFilterFactory {

    constructor(protected readonly dynamicFilterControlManager: DynamicFilterControlManager) {
    }

    schemaToModel<TFilter extends SplineRecord = SplineRecord, TId extends keyof TFilter = keyof TFilter>(
        filterSchema: DynamicFilterSchema<TFilter>,
        defaultFilterValue: Partial<TFilter> = {}): Observable<DynamicFilterModel<TFilter>> {

        //
        // VALIDATE CONTROLS SCHEMA
        //      - check if control type factory exist
        //
        const validFilterControls = filterSchema
            .filter(controlSchema => {
                const controlFactory = this.dynamicFilterControlManager.getFactory(controlSchema.type)

                if (!controlFactory) {
                    console.error(`[DynamicFilter]: FilterControlFactory of type ${controlSchema.type} does not exist.`)
                }
                return !!controlFactory
            })

        // pick just valid schema and create control models with te relevant factory
        const validFilterControlsModelsObservers = validFilterControls
            .map(
                controlSchema => this.controlSchemaToModel<TFilter>(
                    controlSchema, defaultFilterValue[controlSchema.id]
                )
            )

        return (
            validFilterControlsModelsObservers.length
                ? forkJoin(validFilterControlsModelsObservers)
                : of([])
        )
            .pipe(
                map(controlsModelsList => {

                    const filterModel = new DynamicFilterModel<TFilter>(controlsModelsList)
                    // TODO: we need to initialize the default value in one place.
                    if (defaultFilterValue) {
                        filterModel.partialPatchValue(defaultFilterValue, false)
                    }

                    return filterModel
                })
            )

    }

    protected controlSchemaToModel<TFilter extends SplineRecord = SplineRecord, TId extends keyof TFilter = keyof TFilter>(
        controlSchema: DynamicFilterControlSchema<TFilter[TId], unknown, TId>,
        defaultValue?: TFilter[TId]
    ): Observable<IDynamicFilterControlModel<TFilter[TId], unknown, TId>> {

        const controlFactory = this.dynamicFilterControlManager.getFactory(controlSchema.type)

        return controlFactory.createModelFromSchema<TFilter[TId]>(
            controlSchema, defaultValue
        ) as Observable<IDynamicFilterControlModel<TFilter[TId], unknown, TId>>
    }

}
