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




import { DtCellValueControlEvent, DtCellValueSchema, TCellValue } from '../../core'


export namespace DtCellLink {

    export const TYPE = 'Link'

    export type Value = {
        title: string
        routerLink?: any[] | string
        routerQueryParams?: any[] | string
        target?: string
        onClick?: (rowData: any) => void
        emitCellEvent?: (rowData: any) => DtCellValueControlEvent
        description?: string
    }

    export enum LinkStyle {
        Default = 'default',
        Main = 'main',
    }

    export const DEFAULT_LINK_STYLE = LinkStyle.Default

    export type Options = {
        style?: LinkStyle
    }

    export function getColSchema(value: TCellValue<Value>, options?: Options): Partial<DtCellValueSchema<Value>> {
        return {
            type: TYPE,
            value,
            options
        }
    }

}
