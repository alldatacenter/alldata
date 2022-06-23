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

import { keyBy } from 'lodash-es'

import { AttributeDataType, AttributeDataTypeDto, AttributeSchema, AttrSchemasCollection, toAttributeDataType } from '../attribute'

import { Operation, OperationDto, toOperation } from './operation.models'


export type OperationDetails = {
    operation: Operation
    schemas: AttributeSchema[][]
    schemasCollection: AttrSchemasCollection
    output: number // output DS index
    inputs: number[] // output DS index
    dataTypes: AttributeDataType[]
}

export type OperationDetailsDto = {
    operation: OperationDto
    schemas: AttributeSchema[][]
    output: number
    inputs: number[]
    dataTypes: AttributeDataTypeDto[]
}

export function toOperationDetails(entity: OperationDetailsDto): OperationDetails {
    return {
        operation: toOperation(entity.operation),
        schemas: entity.schemas,
        schemasCollection: keyBy(entity.schemas.reduce((acc, curr) => [...acc, ...curr], []), 'id'),
        output: entity.output,
        inputs: entity.inputs,
        dataTypes: entity.dataTypes.map(item => toAttributeDataType(item)),
    }
}
