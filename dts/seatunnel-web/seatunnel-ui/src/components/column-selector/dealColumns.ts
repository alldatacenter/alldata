/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { h } from 'vue'
import { NCheckbox } from 'naive-ui'

interface columnType {
  key: string
  title: string
  type?: string
  [key: string]: any
}
interface optionType {
  label: string
  value: string
  disabled?: () => boolean
  [key: string]: any
}
const copyKey = ['copy']

function isCopyKyeOrSelection(
  item: columnType,
  selectArray: Array<any>
): boolean {
  return (
    (copyKey.indexOf(item.key) > -1 && selectArray.indexOf('name') > -1) ||
    item.type == 'selection' ||
    item.key == 'operation'
  )
}

export function filterColumns(
  selectArray: Array<string | number> | null,
  oriArray: Array<columnType>
): Array<columnType> {
  if (!selectArray || selectArray.length == 0) {
    return []
  }

  const resArray: Array<columnType> = [] as Array<columnType>
  oriArray.forEach((item: any) => {
    if (
      isCopyKyeOrSelection(item, selectArray) ||
      selectArray.indexOf(item.key) > -1
    ) {
      resArray.push(item)
    }
  })
  return resArray
}

export function getColumnsBytable(
  tableColumns: Array<any>,
  t: any
): Array<any> {
  const resArray: Array<optionType> = [
    {
      label: t('project.all_column'),
      value: 'ALL',
      key: 'ALL',
      type: 'render',
      render: () => {
        return h(NCheckbox, {
          value: 'ALL',
          label: t('project.all_column'),
          class: 'checkbox'
        })
      }
    }
  ]
  tableColumns.forEach((item: any) => {
    if (!isCopyKyeOrSelection(item, ['name'])) {
      resArray.push({
        ...item,
        label: item.title,
        value: item.key,
        type: 'render',
        render: () => {
          return h(NCheckbox, {
            value: item.key,
            label: item.title,
            class: 'checkbox'
          })
        }
      })
    }
  })
  return resArray
}

export function getCheckboxList(
  selectorColumns: Array<columnType>,
  localColumn: Array<columnType>,
  ALL_CONF = false
): Array<string> {
  const checkboxList: Array<string> = []
  const localCheckboxList: Array<string> = []
  localColumn.forEach((el: any) => {
    if (!isCopyKyeOrSelection(el, ['name'])) localCheckboxList.push(el.key)
  })
  if (ALL_CONF || selectorColumns.length == localCheckboxList.length + 1) {
    selectorColumns.forEach((item: any) => {
      checkboxList.push(item.key)
    })
  } else {
    checkboxList.push(...localCheckboxList)
  }
  return checkboxList
}


export function setColumns(key: string, value: any) {
  localStorage.setItem(`col_${key}`, JSON.stringify(value))
}

export function getColumns(key: string): Array<any> {
  return JSON.parse(localStorage.getItem(`col_${key}`) as string) as Array<any>
}
