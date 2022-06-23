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

export type PrimitiveNotEmpty = number | string | boolean

export type SplineRecord<T = any, K extends keyof any = string> = Record<K, T>

export type SplineDateRangeValue<TDate = Date> = {
    dateFrom: TDate | null
    dateTo: TDate | null
}

export type GenericEvent<TData extends SplineRecord = SplineRecord> = {
    eventName: string
    data?: TData
}

export type NamedHref = {
    name: string
    href: string
}
