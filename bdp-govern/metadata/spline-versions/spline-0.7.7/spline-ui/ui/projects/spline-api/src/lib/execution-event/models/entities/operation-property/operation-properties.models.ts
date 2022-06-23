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

import { OpExpression } from './expression/op-expression.models'


export type OperationProperties =
    & (Record<string, any>)

export type OperationPropertiesAlias =
    & OperationProperties
    &
    {
        name: string
    }

export type OperationPropertiesAggregate =
    & OperationProperties
    &
    {
        aggregateExpressions: OpExpression[]
        groupingExpressions: OpExpression[]
    }

export type OperationPropertiesFilter =
    & OperationProperties
    &
    {
        condition: OpExpression
    }

export type OperationPropertiesJoin =
    & OperationProperties
    &
    {
        condition: OpExpression
        joinType: string
    }

export type OperationPropertiesProjection =
    & OperationProperties
    &
    {
        projectList: OpExpression[]
    }

export type OperationPropertiesRead =
    & OperationProperties
    &
    {
        inputSources: string[]
        sourceType: string
    }

export type OperationPropertiesSort =
    & OperationProperties
    &
    {
        order: Array<{
            expression: OpExpression
            direction: string
        }>
    }

export type OperationPropertiesWrite =
    & OperationProperties
    &
    {
        append: boolean
        outputSource: string
        destinationType: string
    }
