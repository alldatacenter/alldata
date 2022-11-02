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

Ember.Handlebars.registerHelper('timeAgo', function(property, options) {
  var normalizePath = Em.Handlebars.normalizePath(((options.contexts && options.contexts[0]) || this), property, options.data);

  var boundView = Em._HandlebarsBoundView.extend({
    path: normalizePath.path,
    pathRoot: normalizePath.root,
    isEscaped: !options.hash.unescaped,
    templateData: options.data,
    shouldDisplayFunc:function () {
      return true;
    },
    normalizedValue:function() {
      var value = Em._HandlebarsBoundView.prototype.normalizedValue.call(this);

      return function(value, options) {
        return (moment.isMoment(value))?moment(value).fromNow(false):value;
      }.call(this, value, options);
    }
  });

  boundView.reopen({
    didInsertElement: function() {
      if (this.updateTimer) {
        Em.run.cancel(this.updateTimer);
      }

      Em.run.later(this,function() {
        if (this._state === 'inDOM') {
          Em.run.scheduleOnce('render', this, 'rerender');
        }
      },30000);
    },
    willDestroyElement: function() {
      if (this.updateTimer) {
        Em.run.cancel(this.updateTimer);
      }
    },
    updateValue:function () {
      Em.run.scheduleOnce('render', this, 'rerender');
    }.observes(normalizePath.path)
  });

  options.data.view.appendChild(boundView);
});
