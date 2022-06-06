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

require('views/common/quick_view_link_view');

/**
 * This view helps to get and format correct link to LogSearch UI with query params.
 * Used mostly for navigation to concrete log file.
 *
 * @augments App.QuickLinksView
 */
App.LogSearchUILinkView = App.QuickLinksView.extend({
  content: function() {
    return App.Service.find().findProperty('serviceName', 'LOGSEARCH');
  }.property(),

  formatedLink: function() {
    if (this.get('quickLinks.length')) {
      return Em.get(this.get('quickLinks').findProperty('label', 'Log Search UI'), 'url') + this.get('linkQueryParams');
    }
    return false;
  }.property('quickLinks.length', 'linkQueryParams')
});
