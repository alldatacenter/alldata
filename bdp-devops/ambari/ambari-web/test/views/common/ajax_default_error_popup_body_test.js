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
require('views/common/ajax_default_error_popup_body');

describe('App.AjaxDefaultErrorPopupBodyView', function () {

  describe('#statusCode', function () {

    var view = App.AjaxDefaultErrorPopupBodyView.create();

    it('should format status code', function () {
      view.set('status', 404);
      expect(view.get('statusCode')).to.equal(Em.I18n.t('utils.ajax.defaultErrorPopupBody.statusCode').format(404));
    });

  });

  describe('#showMessage', function () {

    var view = App.AjaxDefaultErrorPopupBodyView.create(),
      title = 'should be {0}',
      cases = [
        {
          message: 'error',
          showMessage: true
        },
        {
          message: '',
          showMessage: false
        },
        {
          message: null,
          showMessage: false
        },
        {
          message: undefined,
          showMessage: false
        },
        {
          message: 0,
          showMessage: false
        }
      ];

    cases.forEach(function (item) {
      it(title.format(item.showMessage), function () {
        view.set('message', item.message);
        expect(view.get('showMessage')).to.equal(item.showMessage);
      });
    });

  });

  describe('#api', function () {

    var view = App.AjaxDefaultErrorPopupBodyView.create();

    it('should format string with request type and URL', function () {
      view.setProperties({
        type: 'GET',
        url: 'api/v1/clusters'
      });
      expect(view.get('api')).to.equal(Em.I18n.t('utils.ajax.defaultErrorPopupBody.message').format('GET', 'api/v1/clusters'));
    });

  });

});
