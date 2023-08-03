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
import { NEllipsis } from 'naive-ui'
import ButtonLink from '@/components/button-link'
// import { IPermissionModule, usePermissionStore } from '@/store/user'

interface ITableLinkParams {
  title: string
  key: string
  width?: number
  isCopy?: boolean
  sorter?: string
  minWidth?: string | number
  ellipsis?: any
  textColor?: (rowData: any) => string
  showEmpty?: boolean
  button: {
    permission?: string
    getHref?: (rowData: any) => string | null
    showText?: (rowData: any) => boolean
    onClick?: (rowData: any) => void
    disabled?: (rowData: any) => boolean
  }
}

export const useTableLink = (
  params: ITableLinkParams,
  // module?: IPermissionModule
) => {
  // const functionPermissions = usePermissionStore().getPermissions(
  //   module || 'common',
  //   'function'
  // )
  // const routerPermissions = usePermissionStore().getPermissions(
  //   module || 'common',
  //   'router'
  // )

  const getButtonVnode = (rowData: any) => {
    const maxWidth = params.width ? params.width - 24 : params.width
    const textColor = params.textColor ? params.textColor(rowData) || '' : ''
    return h(
          ButtonLink,
          {
            href: params.button.getHref ? params.button.getHref(rowData) : null,
            onClick: () =>
              params.button.onClick && params.button.onClick(rowData),
            disabled: params.button.disabled && params.button.disabled(rowData)
          },
          {
            default: () =>
              h(
                NEllipsis,
                { style: `max-width: ${maxWidth}px; color: ${textColor}` },
                () => {
                  if (!params.showEmpty) {
                    return rowData[params.key]
                  } else {
                    return rowData[params.key] || '-'
                  }
                }
              )
          }
        )
    
  }

  return {
    title: params.title,
    key: params.key,
    titleColSpan: params.isCopy ? 2 : 1,
    width: params.width || '',
    minWidth: params.minWidth || '',
    sorter: params.sorter,
    ellipsis: params.ellipsis || {},
    render: (rowData: any) => getButtonVnode(rowData)
  }
}
