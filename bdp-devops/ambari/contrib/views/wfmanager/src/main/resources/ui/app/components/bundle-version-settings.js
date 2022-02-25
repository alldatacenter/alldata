/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';
import Constants from '../utils/constants';
import SchemaVersions from '../domain/schema-versions';

export default Ember.Component.extend({
  schemaVersions : SchemaVersions.create({}),
  initialize : function(){
    this.set('bundleSchemaVersions', this.get('schemaVersions').getSupportedVersions('bundle'));
    this.set('selectedBundleVersion', this.get('bundle').schemaVersions.bundleVersion);
  }.on('init'),
  rendered : function(){
    this.$('#version-settings-dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#version-settings-dialog').modal('show');
    this.$('#version-settings-dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('showVersionSettings', false);
    }.bind(this));
  }.on('didInsertElement'),
  actions : {
    save (){
      this.get('bundle').schemaVersions.bundleVersion = this.get('selectedBundleVersion');
      this.$('#version-settings-dialog').modal('hide');
    },
    cancel (){
      this.$('#version-settings-dialog').modal('hide');
    }
  }
});
