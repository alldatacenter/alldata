/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ConfigDiffView = Em.View.extend({
  template: Em.Handlebars.compile('{{view.diff}}'),
  diff: function () {
    var trimAndSort = function (value) {
      if (value == null) {
        return [];
      }
      var values = value.split("\n").filter(function (item) {
        return item != "";
      }).sort().join("\n");
      return difflib.stringAsLines(values);
    };
    var initialValues = trimAndSort(this.get('config.initialValue')),
      recommendedValues = trimAndSort(this.get('config.recommendedValue')),
      opcodes = new difflib.SequenceMatcher(initialValues, recommendedValues).get_opcodes();
    if (initialValues.length === 1 && recommendedValues.length === 1) {
      // changes in properties with single-line values shouldn't be highlighted
      opcodes[0][0] = 'equal';
    }
    if (!initialValues.length) {
      if (recommendedValues.length > 1) {
        // initial and recommended values should have the same number of rows
        initialValues = Array(recommendedValues.length - 1).join('.').split('.');
      }
      initialValues.unshift(Em.I18n.t('popup.dependent.configs.table.undefined'));
      opcodes[0][0] = 'not-defined'; // class name for cell corresponding to undefined property
      opcodes[0][2] = recommendedValues.length; // specifying rows number explicitly to avoid omitting of 'Property undefined' message
    }
    if (!recommendedValues.length) {
      if (initialValues.length > 1) {
        // initial and recommended values should have the same number of rows
        recommendedValues = Array(initialValues.length - 1).join('.').split('.');
      }
      recommendedValues.unshift(Em.I18n.t('popup.dependent.configs.table.removed'));
      opcodes[0][0] = 'is-removed'; // class name for cell corresponding to removed property
      opcodes[0][4] = initialValues.length; // specifying rows number explicitly to avoid omitting of 'Property removed' message
    }
    return new Handlebars.SafeString(diffview.buildView({
      baseTextLines: initialValues,
      newTextLines: recommendedValues,
      opcodes: opcodes
    }).outerHTML);
  }.property('config.initialValues', 'config.recommendedValues')
});
