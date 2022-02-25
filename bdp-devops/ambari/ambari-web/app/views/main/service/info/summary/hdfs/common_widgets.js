/**
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

const numberUtils = require('utils/number_utils');

function diskPart(i18nKey, totalKey, usedKey) {
  return Em.computed(totalKey, usedKey, function () {
    const text = Em.I18n.t(i18nKey),
      total = this.get(totalKey),
      used = this.get(usedKey);
    return text.format(numberUtils.bytesToSize(used, 1, 'parseFloat'), numberUtils.bytesToSize(total, 1, 'parseFloat'));
  });
}

function diskPartPercent(i18nKey, totalKey, usedKey) {
  return Em.computed(totalKey, usedKey, function () {
    const text = Em.I18n.t(i18nKey),
      total = this.get(totalKey),
      used = this.get(usedKey);
    let percent = total > 0 ? ((used * 100) / total).toFixed(2) : 0;
    if (percent == 'NaN' || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + ' ';
    }
    return text.format(percent);
  });
}

/**
 * Metric widgets which are common for all namespaces
 */
App.HDFSSummaryCommonWidgetsView = Em.View.extend(App.HDFSSummaryWidgetsMixin, {

  templateName: require('templates/main/service/info/summary/hdfs/common_widgets'),

  dfsUsedDiskPercent: diskPartPercent(
    'dashboard.services.hdfs.capacityUsedPercent', 'model.capacityTotal', 'model.capacityUsed'
  ),

  dfsUsedDisk: diskPart(
    'dashboard.services.hdfs.capacityUsed', 'model.capacityTotal', 'model.capacityUsed'
  ),

  nonDfsUsed: function () {
    const total = this.get('model.capacityTotal'),
      remaining = this.get('model.capacityRemaining'),
      dfsUsed = this.get('model.capacityUsed');
    return (Em.isNone(total) || Em.isNone(remaining) || Em.isNone(dfsUsed)) ? null : total - remaining - dfsUsed;
  }.property('model.capacityTotal', 'model.capacityRemaining', 'model.capacityUsed'),

  nonDfsUsedDiskPercent: diskPartPercent(
    'dashboard.services.hdfs.capacityUsedPercent', 'model.capacityTotal', 'nonDfsUsed'
  ),

  nonDfsUsedDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'model.capacityTotal', 'nonDfsUsed'),

  remainingDiskPercent: diskPartPercent(
    'dashboard.services.hdfs.capacityUsedPercent', 'model.capacityTotal', 'model.capacityRemaining'
  ),

  remainingDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'model.capacityTotal', 'model.capacityRemaining')

});
