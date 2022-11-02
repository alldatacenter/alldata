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

import { omit } from 'lodash-es'

import { AttributeDtType } from './attribute-dt-type.models'


export type AttributeDataType =
    | AttributeDataTypeSimple
    | AttributeDataTypeStruct
    | AttributeDataTypeArray

export type AttributeDataTypeBase = {
    id: string
    nullable: boolean
    type: AttributeDtType
}


export type AttributeDataTypeSimple =
    & AttributeDataTypeBase
    &
    {
        name: string
    }

export type AttributeDataTypeStruct =
    & AttributeDataTypeBase
    &
    {
        fields: AttributeDataTypeField[]
    }

export type AttributeDataTypeArray =
    & AttributeDataTypeBase
    &
    {
        elementDataTypeId: string
    }


export type AttributeDataTypeDto = {
    id: string
    nullable: boolean
    _type?: string
    _typeHint?: string
    name?: string
    elementDataTypeId?: string
    fields?: AttributeDataTypeField[]
}


export type AttributeDataTypeField = {
    dataTypeId: string
    name: string
}

export function toAttributeDataType(entity: AttributeDataTypeDto): AttributeDataType {
    return {
        ...(omit<AttributeDataTypeDto, '_type' | '_typeHint'>(entity, ['_type'], '_typeHint')),
        type: extractAttributeDtType(entity),
    } as AttributeDataType
}


function extractAttributeDtType(entity: AttributeDataTypeDto): AttributeDtType {
    const typeSource = entity._type ?? entity._typeHint
    for (const value of Object.values(AttributeDtType)) {
        if (typeSource.includes(value)) {
            return value
        }
    }
    return AttributeDtType.Struct
}
