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

App.ScriptJobView = Em.View.extend({
  collapsePanel: Em.View.extend({
    actions:{
      toggleCollapse:function () {
        this.toggleProperty('collapsed');
      }
    },
    collapsed:true,
    collapseControl: function () {
      this.$('.collapse').collapse(this.get('collapsed') ? 'hide' : 'show');
    }.observes('collapsed'),
    initCollapse:function () {
      this.$('.collapse').collapse({
        toggle: !this.get('collapsed')
      })
    }.on('didInsertElement'),
    hasEditor:false,
    fixEditor:function () {
      if (this.get('hasEditor')) {
        this.$().on('shown.bs.collapse', function (e) {
          var cme = this.$('.CodeMirror').get(0);
          if (cme && cme.CodeMirror) {
            cme.CodeMirror.setSize(null, this.$('.editor-container').height());
          }
        }.bind(this));
      }
    }.on('didInsertElement')
  }),
  bindTooltips:function () {
    $('.fullscreen-toggle').tooltip();
  }.on('didInsertElement')
});
