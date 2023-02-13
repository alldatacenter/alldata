/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import i18n from '@/i18n';

export abstract class DataStatic {
  static I18nMap: Record<string, string> = {};

  static I18n(i18nkey: string): PropertyDecorator {
    return (target: any, propertyKey: string) => {
      const { I18nMap } = target.constructor;
      target.constructor.I18nMap = {
        ...I18nMap,
        [propertyKey]: i18nkey.indexOf('.') !== -1 ? i18n.t(i18nkey) : i18nkey,
      };
    };
  }
}
