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

App.ButtonProgressView = Em.View.extend(Em.TargetActionSupport, {
  layoutName: require('templates/common/button_progress'),
  isDisabled: Em.computed.or('disabled', 'isInProgress'),
  /**
   * Target to perform `onClick` function default to App.router
   * @type {Em.Object|Em.View|Em.Controller}
   */
  target: null,
  /**
   * Property determines progress state
   * @type {Boolean}
   */
  isInProgress: null,
  /**
   * on click handler
   * @type {Function}
   */
  action: null,
  /**
   * When true spinner appears to right side, when false - to left
   * @type {Boolean}
   */
  doSpinRight: true,

  targetObject: function() {
    var target = this.get('target'),
        splitted;
    if (!target) {
      return this.get('controller.target');
    } else if (typeof target === 'string') {
      splitted = target.split('.');
      if (splitted[0] === 'view') {
        splitted = ['parentView'].concat(splitted.slice(1));
      }
      return Em.get(this, splitted.join('.'))
    } else {
      return target;
    }
  }.property('target'),

  handleClick: function() {
    if (this.get('isDisabled')) {
      return;
    }
    var target = this.get('targetObject');
    var targetMethod = this.get('action');
    if (target.isState && typeof target.send === 'function') {
      target.send(targetMethod);
    } else if (targetMethod && typeof targetMethod === 'function') {
      targetMethod.apply(target);
    } else if (typeof targetMethod === 'string' && typeof Em.get(target, targetMethod) === 'function') {
      Em.get(target, targetMethod).call(target);
    } else {
      Ember.Logger.error('Cannot invoke action %s on target %s', targetMethod, target);
    }
  }
});
