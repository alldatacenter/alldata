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

/**
 * Same as Em.TextArea
 * Update self height (to avoid scrolling) when value is changed
 *
 * @type {Em.View}
 */
App.NotScrollableTextArea = Em.TextArea.extend({

  didInsertElement: function() {
    this.fitHeight();
    this.$().select();
  },

  fitHeight: function () {
    var self = this.$();
    if (self) {
      Em.run.next(function () {
        self.height(1);
        self.height(self[0].scrollHeight);
      });
    }
  }.observes('value')

});