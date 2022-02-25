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

describe('App.LogSearchUILinkView', function() {
  var view;
  beforeEach(function () {
    view = App.LogSearchUILinkView.create({});
  });

  describe('#content', function () {
    it('should keep LOGSEARCH service if it present', function () {
      var servicesArr = [
        Em.Object.create({serviceName: 'HDFS'}),
        Em.Object.create({serviceName: 'LOGSEARCH'}),
        Em.Object.create({serviceName: 'HIVE'}),
      ];
      sinon.stub(App.Service, 'find').returns(servicesArr);
      expect(view.get('content')).to.be.eql(servicesArr[1]);
      App.Service.find.restore();
    });

    it('should keep null if LOGSEARCH is not present', function () {
      var servicesArr = [
        Em.Object.create({serviceName: 'HDFS'}),
        Em.Object.create({serviceName: 'HIVE'}),
      ];
      sinon.stub(App.Service, 'find').returns(servicesArr);
      expect(view.get('content')).to.be.equal(undefined);
      App.Service.find.restore();
    });
  });

  describe('#formatedLink', function () {
    it('should be false if quickLinks is empty', function () {
      view.set('quickLinks', []);
      expect(view.get('formatedLink')).to.be.equal(false);
    });

    it('should be equal to url of quickLink of logsearch and quiclLinksParams', function () {
      view.set('quickLinks', [
        Em.Object.create({'label': 'Log Search UI', url: 'http://logsearchling.com'}),
        Em.Object.create({'label': 'test 1', url: 'http://test1.com'}),
        Em.Object.create({'label': 'test 2', url: 'http://test2.com'}),
      ]);
      view.set('linkQueryParams', '?param1=p1&param2=p2');
      expect(view.get('formatedLink')).to.be.equal('http://logsearchling.com?param1=p1&param2=p2');
    });
  });
});