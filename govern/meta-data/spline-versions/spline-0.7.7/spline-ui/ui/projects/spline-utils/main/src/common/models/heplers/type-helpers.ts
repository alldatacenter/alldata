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

export namespace TypeHelpers {

    export function isBoolean(value: any): value is boolean {
        return typeof value === 'boolean'
    }

    export function isArray(value: any): value is Array<any> {
        return Array.isArray(value)
    }

    export function isFunction(value: any): value is Function {
        return typeof value === 'function'
    }

    export function isNumber(value: any): value is number {
        return typeof value === 'number'
    }

    export function isObject(value: any): value is object {
        if (value === null) {
            return false
        }
        return typeof value === 'object' && Object.prototype.toString.call(value) !== '[object Array]'
    }

    export function isString(value: any): value is string {
        return typeof value === 'string'
    }

    export function isEmpty(value: any): boolean {
        if (isArray(value)) {
            return value.length === 0
        }
        else if (isObject(value)) {
            return Object.keys(value).length === 0
        }
        else {
            return value === undefined || value === null
        }
    }
}
