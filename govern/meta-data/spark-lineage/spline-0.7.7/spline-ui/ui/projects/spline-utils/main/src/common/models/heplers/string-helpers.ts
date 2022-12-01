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

import { TypeHelpers } from './type-helpers'


export namespace StringHelpers {

    export function camelCaseToDashCase(str: string): string {
        const regex = new RegExp('([a-z])([A-Z])', 'g')
        return str.replace(regex, '$1-$2').toLowerCase()
    }

    export function camelCaseToSnakeCase(str: string, makeUppercase: boolean = false): string {
        const regex = new RegExp('([a-z])([A-Z])', 'g')
        const snackCaseStr = str.replace(regex, '$1_$2')
        return makeUppercase
            ? snackCaseStr.toUpperCase()
            : snackCaseStr.toLowerCase()
    }

    export function guid(): string {
        function s4(): string {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1)
        }

        return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4()
    }

    export function numberToString(num: number, minLength = 2): string {
        let resString = num.toString()

        while (resString.length < minLength) {
            resString = '0' + resString
        }

        return resString
    }

    export function toStringValue(value: number | Array<any> | object | {}): string {
        if (TypeHelpers.isString(value)) {
            return value
        }
        else if (TypeHelpers.isNumber(value) || TypeHelpers.isBoolean(value)) {
            return value.toString()
        }
        else if (TypeHelpers.isArray(value)) {
            return value.join(', ')
        }
        else if (TypeHelpers.isObject(value)) {
            return JSON.stringify(value)
        }
        else {
            return ''
        }
    }

    export function snakeCaseToCamelCase(str: string): string {
        const regex = new RegExp('([-_][a-z])', 'ig')
        return str.toLowerCase().replace(regex, ($1) => {
            return $1.toUpperCase()
                .replace('-', '')
                .replace('_', '')
        })
    }

    export function encodeObjToUrlString(obj: Record<string, any>): string {
        return encodeBase64(JSON.stringify(obj))
    }

    export function decodeObjFromUrlString<T extends Record<string, any> = Record<string, any>>(str: string): T {
        return JSON.parse(decodeBase64(str)) as T
    }

    export function encodeBase64(str: string): string {
        return window.btoa(str)
    }

    export function decodeBase64(str: string): string {
        return window.atob(str)
    }

}
