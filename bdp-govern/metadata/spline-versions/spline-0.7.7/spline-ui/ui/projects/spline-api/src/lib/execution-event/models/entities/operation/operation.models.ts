/*
 * Copyright 2020 ABSA Group Limited
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

import { OperationProperties } from '../operation-property'

import { OperationType } from './operation-type.models'


export type Operation = {
    id: string
    type: OperationType | string
    name: string
    properties: OperationProperties
}


export type OperationDto = {
    _id: string
    _type: OperationType | string
    name: string
    properties: OperationProperties
}

export function toOperation(entity: OperationDto): Operation {
    return {
        id: entity._id,
        type: entity._type,
        name: entity.name,
        properties: entity.properties,
    }
}
