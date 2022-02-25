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



describe('App.FormatNullView', function () {
  var view;
  beforeEach(function () {
    view = App.FormatNullView.create({});
  });

  describe('#result', function () {
    var testCases = [{
      msg: 'It should return notAvailable string if content is null and no emtpy value provided',
      emptyValue: null,
      content: null,
      result: Em.I18n.t('services.service.summary.notAvailable')
    },{
      msg: 'It should return emptyValue string if content is null and not started with t: prefix',
      emptyValue: 'test',
      content: null,
      result: 'test'
    }, {
      msg: 'It should return translated emptyValue string if content is null and started wit t: prefix',
      emptyValue: 't:services.service.summary.diskInfoBar.used',
      content: null,
      result:  Em.I18n.t('services.service.summary.diskInfoBar.used')
    }, {
      msg: 'It should return content if it is truthly',
      emptyValue: 't:services.service.summary.diskInfoBar.used',
      content: '1',
      result:  '1'
    }, {
      msg: 'It should return content if it is 0',
      emptyValue: 't:services.service.summary.diskInfoBar.used',
      content: 0,
      result:  0
    }];
    testCases.forEach(function (item) {
      it(item.msg, function () {
        view.set('empty', item.emptyValue);
        view.set('content', item.content);
        expect(view.get('result')).to.be.equal(item.result);
      });
    });
  });
});