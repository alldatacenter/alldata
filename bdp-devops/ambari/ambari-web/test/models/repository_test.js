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

require('models/repository');

function getModel() {
  return App.Repository.createRecord();
}

describe('App.Repository', function () {

  var model;

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedNotEqualProperties(getModel(), 'undo', 'baseUrl', 'baseUrlInit');

  App.TestAliases.testAsComputedAlias(getModel(), 'isSelected', 'operatingSystem.isSelected', 'boolean');

  App.TestAliases.testAsComputedAlias(getModel(), 'clearAll', 'baseUrl', 'string'); // string??

  describe('#invalidFormatError', function () {

    var cases = [
      {
        baseUrl: 'http://domain-name_0.com/path/subpath?p0=v0&p1=v1@v2.v3#!~hash0,(hash1)+hash2[hash3]/*;hash_4%2F',
        invalidFormatError: false,
        title: 'valid http url'
      },
      {
        baseUrl: 'https://domain.com/path?p=v',
        invalidFormatError: false,
        title: 'valid https url'
      },
      {
        baseUrl: 'ftp://domain.com:123',
        invalidFormatError: false,
        title: 'valid ftp url'
      },
      {
        baseUrl: 'ftp://user_:password0@domain.com',
        invalidFormatError: false,
        title: 'valid ftp url with authorization'
      },
      {
        baseUrl: 'ftp://user :password/@domain.com',
        invalidFormatError: true,
        title: 'ftp url with disallowed characters'
      },
      {
        baseUrl: 'http://domain.com:/path',
        invalidFormatError: true,
        title: 'no port specified when expected'
      },
      {
        baseUrl: 'file://etc/file.repo',
        invalidFormatError: false,
        title: 'valid Unix file url'
      },
      {
        baseUrl: 'file:///etc/file.repo',
        invalidFormatError: false,
        title: 'valid Unix file url (3 slashes)'
      },
      {
        baseUrl: 'file://c:/file.repo',
        invalidFormatError: false,
        title: 'valid Windows file url'
      },
      {
        baseUrl: 'file:///c:/file.repo',
        invalidFormatError: false,
        title: 'valid Windows file url (3 slashes)'
      },
      {
        baseUrl: 'file://c|/file.repo',
        invalidFormatError: false,
        title: 'valid Windows file url (| separator)'
      },
      {
        baseUrl: 'file://C:/file.repo',
        invalidFormatError: false,
        title: 'valid Windows file url (capital drive char)'
      },
      {
        baseUrl: 'file://etc /file.repo',
        invalidFormatError: true,
        title: 'file url with disallowed characters'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        model.set('baseUrl', item.baseUrl);
        expect(model.get('invalidFormatError')).to.equal(item.invalidFormatError);
      });
    });

  });

});
