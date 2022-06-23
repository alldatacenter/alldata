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


import { isPlainObject } from 'lodash-es'
import { AttrSchemasCollection, OpExpression } from 'spline-api'
import { SplineColors } from 'spline-common'
import { SdWidgetExpansionPanel, SdWidgetJson, SdWidgetSchema, SdWidgetSimpleRecord } from 'spline-common/data-view'
import { SdWidgetExpression } from 'spline-shared/expression'
import { PrimitiveNotEmpty } from 'spline-utils'


export namespace EventOperationProperty {

    export type NativeProperties = Record<string, any | OrderSpecPropRecord[]>

    export type OrderSpecPropRecord = {
        direction: string
        expression: OpExpression
        nullOrdering?: string
    }

    export type ExtraPropertyValue<T> = {
        label: string
        value: T
    }

    export type OpExpressionInfo = {
        expression: OpExpression
        prefix?: string
        suffix?: string
    }

    export type ExtraPropertyValuePrimitive = ExtraPropertyValue<PrimitiveNotEmpty>
    export type ExtraPropertyValueExpression = ExtraPropertyValue<OpExpressionInfo[]>
    export type ExtraPropertyValueJson = ExtraPropertyValue<any[] | Record<any, any>>

    export type ExtraProperties = {
        primitive: ExtraPropertyValuePrimitive[]
        expression: ExtraPropertyValueExpression[]
        json: ExtraPropertyValueJson[]
    }

    export function parseExtraOptions(nativeProperties: NativeProperties): ExtraProperties {
        return Object.entries(nativeProperties)
            .filter(([, v]) => v != null)
            .reduce(
                (acc, [key, value]) => {
                    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
                        const primitiveValue: ExtraPropertyValuePrimitive = {
                            label: key,
                            value: value,
                        }
                        return { ...acc, primitive: [...acc.primitive, primitiveValue] }
                    }
                    else if (isPlainObject(value) && isExpresionProperty(value)) {
                        return { ...acc, expression: [...acc.expression, toExpressionValue(key, [value])] }
                    }
                    else if (typeof Array.isArray(value) && value.length && isExpresionProperty(value[0])) {
                        return { ...acc, expression: [...acc.expression, toExpressionValue(key, value)] }
                    }
                    else if (typeof Array.isArray(value) && value.length && isOrderSpecProperty(value[0])) {
                        return { ...acc, expression: [...acc.expression, toOrderSpecValue(key, value)] }
                    }
                    else {
                        const jsonValue: ExtraPropertyValueJson = {
                            label: humanizeCase(key),
                            value,
                        }
                        return { ...acc, json: [...acc.json, jsonValue] }
                    }
                },
                {
                    primitive: [],
                    expression: [],
                    json: [],
                },
            )
    }

    export function primitivePropsToDvs(props: ExtraPropertyValuePrimitive[]): SdWidgetSchema[] {
        return [
            SdWidgetSimpleRecord.toSchema(
                props.map(item => ({
                    label: item.label,
                    value: item.value,
                })),
            ),
        ]
    }

    export function expressionPropsToDvs(props: ExtraPropertyValueExpression[],
                                         attrSchemasCollection: AttrSchemasCollection): SdWidgetSchema[] {
        return props.map(
            item => SdWidgetExpansionPanel.toSchema(
                {
                    title: item.label,
                    icon: 'code',
                    iconColor: SplineColors.BLUE,
                },
                item.value.map(
                    expressionInfo => SdWidgetExpression.toSchema({
                        expression: expressionInfo.expression,
                        prefix: expressionInfo.prefix,
                        suffix: expressionInfo.suffix,
                        attrSchemasCollection
                    }),
                ),
            ),
        )
    }

    export function jsonPropsToDvs(props: ExtraPropertyValueJson[]): SdWidgetSchema[] {
        return props.map(
            item => SdWidgetExpansionPanel.toSchema(
                {
                    title: item.label,
                    icon: 'code-json',
                    iconColor: SplineColors.BLUE,
                },
                [
                    SdWidgetJson.toSchema(item.value),
                ],
            ),
        )

    }

    function toExpressionValue(key: string, expressionsList: OpExpression[]): ExtraPropertyValueExpression {
        return {
            label: humanizeCase(key),
            value: expressionsList.map(expression => ({expression})),
        }
    }

    function toOrderSpecValue(key: string, orderSpecValuesList: OrderSpecPropRecord[]): ExtraPropertyValueExpression {
        return {
            label: humanizeCase(key),
            value: orderSpecValuesList
                .map(orderSpec => ({
                    expression: orderSpec.expression,
                    suffix: `${orderSpec.direction} ${orderSpec.nullOrdering}`
                })),
        }
    }

    function humanizeCase(str: string): string {
        // "fooBar-Baz_Qux" => "Foo Bar Baz Qux"
        return str
            .replace(/([a-z])([A-Z])/g, '$1 $2')
            .replace(/(\d)([^\d])/g, '$1 $2')
            .replace(/([^\d])(\d)/g, '$1 $2')
            .split(/[\W_]/)
            .map(s => s.substring(0, 1).toUpperCase() + s.substring(1)).join(' ')
    }

    function isExpresionProperty(propertyValue: Record<string, any>): propertyValue is OpExpression {
        return propertyValue._typeHint && (propertyValue._typeHint as string).startsWith('expr.')
    }

    function isOrderSpecProperty(propertyValue: Record<string, any>): propertyValue is OrderSpecPropRecord {
        const propertyValueObj = propertyValue as OrderSpecPropRecord
        return propertyValueObj.direction !== undefined
            && propertyValueObj.expression !== undefined
            && isExpresionProperty(propertyValueObj.expression)
    }

}
