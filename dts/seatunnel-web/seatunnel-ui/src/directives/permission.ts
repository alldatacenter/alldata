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
// import { IPermissionModule, usePermissionStore } from '@/store/user'
import type { Directive } from 'vue'

export const permission: Directive = {
  mounted(el, binding) {
    // const code = binding.value
    // if (!code) return
    // let module = 'common' as IPermissionModule

    // const codeItems = code.split(':')
    // if (codeItems.length > 2 && codeItems[0] === 'project') {
    //   module = 'project'
    // }

    // const functionPermissions = usePermissionStore().getPermissions(
    //   module,
    //   'function'
    // )

    // const hasPermission = functionPermissions.has(code)

    // if (hasPermission) return

    // if (el.parentElement?.children.length === 1) {
    //   el.parentElement.style = 'display: none'
    // }

    // el.parentElement.removeChild(el)
  }
}
