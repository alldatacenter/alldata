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

App.JqueryChosenView = Em.View.extend({
  templateName: require('templates/common/chosen_plugin'),
  tagName: 'select',
  classNames: ['form-control'],
  // This needs to be bound from template
  elementId: '',
  title: '',
  options: [],
  /**
   * @name: selectionObj {Object}
   *  Object =   {
   *    placeholder_text: {String}
   *    no_results_text: {String}
   *    nChangeCallback: {Function}
   *  }
   */
  selectionObj: {},

  didInsertElement: function () {
    var self = this;
    var elementId = "#" + self.get("elementId");
    $(elementId).chosen({
      search_contains: true,
      placeholder_text: self.get('selectionObj.placeholder_text'),
      no_results_text: self.get('selectionObj.no_results_text')
    }).change(self.get('selectionObj.onChangeCallback'));

  }
});