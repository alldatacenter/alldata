/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function usePlaceholder() {
  const { t } = useI18n()
  const catalogPh = computed(() => t('catalog')).value
  const dbNamePh = computed(() => t('databaseName')).value
  const tableNamePh = computed(() => t('tableName')).value
  const optimizerGroupPh = computed(() => t('optimzerGroup')).value
  const resourceGroupPh = computed(() => t('resourceGroup')).value
  const parallelismPh = computed(() => t('parallelism')).value
  const usernamePh = computed(() => t('username')).value
  const passwordPh = computed(() => t('password')).value
  const filterDBPh = computed(() => t('database', 2)).value
  const filterTablePh = computed(() => t('table', 2)).value
  return {
    selectPh: t('selectPlaceholder'),
    inputPh: t('inputPlaceholder'),
    selectClPh: t('selectPlaceholder', { selectPh: catalogPh }),
    selectDBPh: t('selectPlaceholder', { selectPh: dbNamePh }),
    inputDBPh: t('inputPlaceholder', { inputPh: dbNamePh }),
    inputClPh: t('inputPlaceholder', { inputPh: catalogPh }),
    inputTNPh: t('inputPlaceholder', { inputPh: tableNamePh }),
    selectOptGroupPh: t('inputPlaceholder', { inputPh: optimizerGroupPh }),
    resourceGroupPh: t('inputPlaceholder', { inputPh: resourceGroupPh }),
    parallelismPh: t('inputPlaceholder', { inputPh: parallelismPh }),
    usernamePh: t('inputPlaceholder', { inputPh: usernamePh }),
    passwordPh: t('inputPlaceholder', { inputPh: passwordPh }),
    filterDBPh: t('filterPlaceholder', { inputPh: filterDBPh }),
    filterTablePh: t('filterPlaceholder', { inputPh: filterTablePh })
  }
}
