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

var view;

describe('App.FormatWordBreakView', function () {

  beforeEach(function () {
    view = App.FormatWordBreakView.create({});
  });

  describe('#result', function () {

    Em.A([
      {content: 'abc', expected: 'abc'},
      {content: 'a.bc', expected: 'a.<wbr>bc'},
      {content: 'a.b.c', expected: 'a.<wbr>b.<wbr>c'},
      {content: 'a/bc', expected: 'a/<wbr>bc'},
      {content: 'a/b/c', expected: 'a/<wbr>b/<wbr>c'},
      {content: 'a.123456789A123456789B12345.c', expected: 'a.<wbr>123456789<wbr>A123456789<wbr>B12345.<wbr>c'},
      {content: 'a_bc', expected: 'a_<wbr>bc'},
      {content: 'a_b_c', expected: 'a_<wbr>b_<wbr>c'},
      {content: 'a_123456789A123456789B12345_c', expected: 'a_<wbr>123456789<wbr>A123456789<wbr>B12345_<wbr>c'},
      {content: 'a.123456789A123456789B12345_c', expected: 'a.<wbr>123456789<wbr>A123456789<wbr>B12345_<wbr>c'},
      {content: 'a.123456789a123456789b12345_c', expected: 'a.<wbr>123456789a123456789b12345_<wbr>c'},
      {content: 'a.123456789a-23456789b12345_c', expected: 'a.<wbr>123456789a-23456789b12345_<wbr>c'},
      {content: 'a.123456789a 23456789b12345_c', expected: 'a.<wbr>123456789a 23456789b12345_<wbr>c'},
      {content: 'a.123456789a123456789_c', expected: 'a.<wbr>123456789a123456789_<wbr>c'},
    ]).forEach(function (test) {
      var message = 'content: {0}, expected: {1}'.format(JSON.stringify(test.content), JSON.stringify(test.expected));
      it(message, function () {
        view.set('content', test.content);
        expect(view.get('result')).to.be.equal(test.expected);
      });
    });

  });

});