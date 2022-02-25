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

describe('App.LogFileSearchView', function() {
  describe('#serializeFilters', function() {
    var makeLevelItem = function(level, isChecked) {
      return Em.Object.create({
        name: level.toUpperCase(),
        checked: !!isChecked
      });
    };
    var makeSelectedKeyword = function(keyword, isIncluded) {
      return Em.Object.create({
        value: keyword,
        isIncluded: !!isIncluded
      });
    };
    [
      {
        viewContent: {
          keywordsFilterValue: 'some_keyword'
        },
        e: 'keywords=some_keyword'
      },
      {
        viewContent: {
          keywordsFilterValue: 'some_keyword',
          levelsContext: [
            makeLevelItem('debug', true),
            makeLevelItem('error', false),
            makeLevelItem('info', true)
          ]
        },
        e: 'levels=DEBUG,INFO&keywords=some_keyword'
      },
      {
        viewContent: {
          keywordsFilterValue: 'some_keyword',
          dateFromValue: '12/12/2015',
          dateToValue: '14/12/2015',
          levelsContext: [
            makeLevelItem('debug', true),
            makeLevelItem('error', true),
            makeLevelItem('info', true)
          ]
        },
        e: 'levels=DEBUG,ERROR,INFO&keywords=some_keyword&dateTo=12/12/2015&dateFrom=12/12/2015'
      },
      {
        viewContent: {
          keywordsFilterValue: 'some_keyword',
          dateFromValue: '12/12/2015',
          levelsContext: [
            makeLevelItem('debug', true),
            makeLevelItem('error', true),
            makeLevelItem('info', true)
          ]
        },
        e: 'levels=DEBUG,ERROR,INFO&keywords=some_keyword&dateFrom=12/12/2015'
      },
      {
        viewContent: {
          keywordsFilterValue: 'some_keyword',
          dateFromValue: '12/12/2015',
          levelsContext: [
            makeLevelItem('debug', true),
            makeLevelItem('error', true),
            makeLevelItem('info', true)
          ],
          selectedKeywords: [
            makeSelectedKeyword("keyword1", true),
            makeSelectedKeyword("keyword2", true),
            makeSelectedKeyword("keyword3", false)
          ]
        },
        e: 'levels=DEBUG,ERROR,INFO&keywords=some_keyword&dateFrom=12/12/2015&include=keyword1,keyword2&exclude=keyword3'
      }
    ].forEach(function(test) {
      it('validate result: ' + test.e, function() {
        var view = App.LogFileSearchView.extend(test.viewContent).create();
        expect(view.serializeFilters()).to.be.eql(test.e);
        view.destroy();
      })
    });
  });
});
