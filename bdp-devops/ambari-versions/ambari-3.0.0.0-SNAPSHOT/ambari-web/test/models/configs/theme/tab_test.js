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

var App = require('app');

function getModel() {
  return App.Tab.createRecord();
}

describe('App.Tab', function () {

  App.TestAliases.testAsComputedSumBy(getModel(), 'errorsCount', 'sections', 'errorsCount');

  App.TestAliases.testAsComputedIfThenElse(getModel(), 'tooltipMsg', 'isHiddenByFilter', Em.I18n.t('services.service.config.nothing.to.display') , '');

  App.TestAliases.testAsComputedIfThenElseByKeys(getModel(), 'isHiddenByFilter', 'isAdvanced', 'isAdvancedHidden', 'allSectionsAreHiddenByFilter');

});
