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

App.ClickElsewhereMixin = Ember.Mixin.create({
  onClickElsewhere: Ember.K,
  clickHandler: Ember.computed('elsewhereHandler',function() {
    return this.get('elsewhereHandler').bind(this);
  }),
  elsewhereHandler: function(e) {
    var $target, element, thisIsElement;
    element = this.get("element");
    $target = $(e.target);
    thisIsElement = $target.closest(element).length === 1;
    if (!thisIsElement) {
      return this.onClickElsewhere(e);
    }
  },
  didInsertElement: function() {
    this._super.apply(this, arguments);
    return $(window).on("click", this.get("clickHandler"));
  },
  willDestroyElement: function() {
    $(window).off("click", this.get("clickHandler"));
    return this._super.apply(this, arguments);
  }
});
