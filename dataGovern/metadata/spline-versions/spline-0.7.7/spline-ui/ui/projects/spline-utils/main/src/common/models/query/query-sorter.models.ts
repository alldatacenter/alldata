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

import { MatSortable } from '@angular/material/sort'
import { Sort } from '@angular/material/sort/sort'


export namespace QuerySorter {

    export enum SortDir {
        ASC = 'ASC',
        DESC = 'DESC'
    }

    export type FieldSorter<TFiled = string> = {
        field: TFiled
        dir: SortDir
    }

    export function toMatSort<TFiled>(filedSorter: FieldSorter<TFiled>): MatSortable {
        return {
            id: filedSorter.field as unknown as string,
            start: filedSorter.dir === SortDir.ASC ? 'asc' : 'desc',
            disableClear: false
        }
    }

    export function fromMatSort(matSort: Sort): FieldSorter {
        return {
            dir: matSort.direction === 'asc' ? SortDir.ASC : SortDir.DESC,
            field: matSort.active
        }
    }

}
