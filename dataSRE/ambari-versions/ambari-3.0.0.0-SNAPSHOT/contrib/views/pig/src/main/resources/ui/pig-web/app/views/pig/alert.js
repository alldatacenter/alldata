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

App.PigAlertView = Ember.View.extend({
  alertsView : Ember.CollectionView.extend({
    content:Em.computed.alias('controller.content'),
    itemViewClass: Ember.View.extend({
      classNames: ['alert fade in'],
      classNameBindings: ['alertClass'],
      attributeBindings:['dismiss:data-dismiss'],
      dismiss:'alert',
      templateName: 'partials/alert-content',
      didInsertElement:function () {
        this.$().bind('closed.bs.alert', Ember.run.bind(this, 'clearAlert'));

        if (this.get('content.status') != 'error') {
          Ember.run.debounce(this, 'close', 3000);
        }
      },
      close : function () {
        if (Em.isArray(this.$())) {
          this.$().alert('close');
        }
      },
      clearAlert:function () {
        return this.get('controller').send('removeAlertObject',this.get('content'));
      },
      alertClass: function () {
        var classes = {'success':'alert-success','error':'alert-danger','info':'alert-info'};
        return classes[this.get('content.status')];
      }.property('content.status')
    })
  })
});
