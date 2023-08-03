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

import type { FormItemRule } from 'naive-ui'

export function useFormValidate(forms: Array<any>, model: any, t: any) {
  const validate: any = {}

  const setValidate = (validate: any, field: string): object => {
    const data: any = {
      required: validate.required,
      trigger: validate.trigger
    }

    if (validate.type === 'non-empty') {
      data['validator'] = (rule: FormItemRule, value: string) => {
        if (!model[field]) {
          return Error(t(validate.message))
        }
      }
    } else if (validate.type === 'union-non-empty') {
      const fields = validate.fields.splice(
        validate.fields.findIndex((f: any) => f !== field),
        1
      )
      data['validator'] = (rule: FormItemRule, value: string) => {
        const fieldsValidate = fields.map((f: string) => !!model[f])
        if (!value && fieldsValidate.includes(true)) {
          return Error(t(validate.message))
        }
      }
    } else if (validate.type === 'mutually-exclusive') {
      const fields = validate.fields.splice(
        validate.fields.findIndex((f: any) => f !== field),
        1
      )
      data['validator'] = (rule: FormItemRule, value: string) => {
        const fieldsValidate = fields.map((f: string) => !!model[f])
        if (value && fieldsValidate.indexOf(false) < 0) {
          return Error(t(validate.message))
        }
      }
    }

    return data
  }

  forms.forEach((f: any) => {
    if (!f.validate || Object.keys(f.validate).length <= 0) return

    validate[f.field] = setValidate(f.validate, f.field)
  })

  return validate
}
