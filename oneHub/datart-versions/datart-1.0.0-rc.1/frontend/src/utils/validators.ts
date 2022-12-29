/**
 * Datart
 *
 * Copyright 2021
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

export function getPasswordValidator(errorMessage: string) {
  return function (_, value: string) {
    if (value && (value.trim().length < 6 || value.trim().length > 20)) {
      return Promise.reject(new Error(errorMessage));
    }
    return Promise.resolve();
  };
}

export function getConfirmPasswordValidator(
  field: string,
  errorMessage: string,
  confirmErrorMessage: string,
) {
  return function ({ getFieldValue }) {
    return {
      validator(_, value: string) {
        if (value && (value.trim().length < 6 || value.trim().length > 20)) {
          return Promise.reject(new Error(errorMessage));
        }
        if (value && getFieldValue(field) !== value) {
          return Promise.reject(new Error(confirmErrorMessage));
        }
        return Promise.resolve();
      },
    };
  };
}
