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

require('views/main/admin/kerberos/progress_view');

App.KerberosDisableView = App.KerberosProgressPageView.extend({

  didInsertElement: function () {
    this._super();
    this.get('controller').loadStep();
  },

  headerTitle: '',

  noticeCompleted: Em.I18n.t('admin.kerberos.disable.notice.completed'),

  notice: Em.I18n.t('admin.kerberos.disable.inProgress'),

  templateName: require('templates/main/admin/kerberos/disable'),

  msgColor: 'alert-info',

  isSimpleModal: true,

  disableKerberos: true
});
