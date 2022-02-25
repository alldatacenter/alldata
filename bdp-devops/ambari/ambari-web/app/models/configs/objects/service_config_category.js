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

App.ServiceConfigCategory = Ember.Object.extend({
  name: null,
  /**
   *  We cant have spaces in the name as this is being used as HTML element id while rendering. Hence we introduced 'displayName' where we can have spaces like 'Secondary Name Node' etc.
   */
  displayName: null,
  /**
   * check whether to show custom view in category instead of default
   */
  isCustomView: false,
  customView: null,
  /**
   * Each category might have a site-name associated (hdfs-site, core-site, etc.)
   * and this will be used when determining which category a particular property
   * ends up in, based on its site.
   */
  siteFileName: null,
  /**
   * Can this category add new properties. Used for custom configurations.
   */
  canAddProperty: false,

  errorCount: 0,

  isAdvanced : function(){
    var name = this.get('name');
    return name.indexOf("Advanced") !== -1 ;
  }.property('name')
});
