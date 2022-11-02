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

import { DtCellComponent, DtCellDefaultComponent } from './cell'
import { DtHeaderCellComponent, DtHeaderCellDefaultComponent } from './header-cell'
import { DtSecondaryHeaderCellComponent, DtSecondaryHeaderCellDefaultComponent } from './secondary-header-cell'
import { DynamicTableComponent } from './table'


export const dynamicTableComponents: any[] = [
    DynamicTableComponent,
    DtCellComponent,
    DtHeaderCellComponent,
    DtSecondaryHeaderCellComponent,
]

export const dynamicTableEntryComponents: any[] = [
    DtCellDefaultComponent,
    DtHeaderCellDefaultComponent,
    DtSecondaryHeaderCellDefaultComponent,
]

export * from './public-api'
