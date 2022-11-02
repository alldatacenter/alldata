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

import { isEmpty, isPlainObject, join, map } from 'lodash-es'
import {
    AttrSchemasCollection,
    OpExpression,
    OpExpressionAlias,
    OpExpressionAttrRef,
    OpExpressionBinary,
    OpExpressionGeneric,
    OpExpressionGenericLeaf,
    OpExpressionLiteral,
    OpExpressionType,
    OpExpressionUDF,
} from 'spline-api'
import { TypeHelpers } from 'spline-utils'


export namespace SplineExpressionValue {

    export function expressionToString(expr: OpExpression, attrSchemasCollection: AttrSchemasCollection): string {
        switch (expr._typeHint) {
            case OpExpressionType.Literal: {
                const exprLiteral = expr as OpExpressionLiteral
                return exprLiteral.value === null ? 'null' : exprLiteral.value
            }

            case OpExpressionType.Binary: {
                const binaryExpr = expr as OpExpressionBinary
                const leftOperand = expressionToString(binaryExpr.children[0], attrSchemasCollection)
                const rightOperand = expressionToString(binaryExpr.children[1], attrSchemasCollection)
                return `${leftOperand} ${binaryExpr.symbol} ${rightOperand}`
            }

            case OpExpressionType.Alias: {
                const ae = expr as OpExpressionAlias
                return `${expressionToString(ae.child, attrSchemasCollection)} AS  ${ae.alias}`
            }

            case OpExpressionType.UDF: {
                const udf = expr as OpExpressionUDF
                const paramList = map(udf.children, child => expressionToString(child, attrSchemasCollection))
                return `UDF:${udf.name}(${join(paramList, ', ')})`
            }

            case OpExpressionType.AttrRef: {
                const attrRef = expr as OpExpressionAttrRef
                return attrSchemasCollection[attrRef.refId]?.name
            }

            case OpExpressionType.GenericLeaf: {
                return renderAsGenericLeafExpr(expr as OpExpressionGenericLeaf, attrSchemasCollection)
            }

            case OpExpressionType.Generic:
            case OpExpressionType.Untyped: {
                const leafText = renderAsGenericLeafExpr(expr as OpExpressionGenericLeaf, attrSchemasCollection)
                const childrenTextsCollection = ((expr as OpExpressionGeneric)?.children || [])
                    .map(child => expressionToString(child, attrSchemasCollection))
                const childrenTextString = childrenTextsCollection.length ? `(${join(childrenTextsCollection, ', ')})` : ''
                return `${leafText}${childrenTextString}`
            }
        }
    }

    function renderAsGenericLeafExpr(expression: OpExpressionGenericLeaf, attrSchemasCollection: AttrSchemasCollection): string {
        const paramList = expression?.params?.length
            ? expression.params
                .map(
                    (value, name) => {
                        const renderedValue = renderGenericLeafParamValue(value, attrSchemasCollection)
                        return `${name}=${renderedValue}`
                    },
                )
            : []
        return isEmpty(paramList)
            ? expression.name
            : `${expression.name}[${paramList.join(', ')}]`
    }

    function renderGenericLeafParamValue(paramValue: OpExpression | object | number | string,
                                         attrSchemasCollection: AttrSchemasCollection): string {

        if ((paramValue as OpExpression)?._typeHint) {
            return expressionToString(paramValue as OpExpression, attrSchemasCollection)
        }

        if (TypeHelpers.isArray(paramValue)) {
            return `[${paramValue.map(valueItem => renderGenericLeafParamValue(valueItem, attrSchemasCollection)).join(', ')}]`
        }

        if (isPlainObject(paramValue)) {
            const renderedPairs = Object.entries(paramValue)
                .map(
                    ([key, value]) => `${key}: ${renderGenericLeafParamValue(value, attrSchemasCollection)}`,
                )
            return `{${renderedPairs.join(', ')}}`
        }

        if (TypeHelpers.isNumber(paramValue)) {
            return paramValue.toString()
        }

        if (TypeHelpers.isString(paramValue)) {
            return paramValue
        }

        throw new Error('Unknown data type')
    }
}
