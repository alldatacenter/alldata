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
require('views/common/modal_popups/hosts_table_list_popup');

describe('App.showHostsTableListPopup', function () {

  var cases = [
    {
      header: 'h0',
      hostName: 'hn0',
      items: ['i0', 'i1'],
      isObjectsList: false,
      title: 'strings list'
    },
    {
      header: 'h1',
      hostName: 'hn1',
      items: [
        {
          name: 'n0',
          status: 's0'
        },
        {
          name: 'n1',
          status: 's1'
        }
      ],
      isObjectsList: true,
      title: 'objects list'
    }
  ];

  cases.forEach(function (item) {

    describe(item.title, function () {

      var popup;
      var popupBody;

      beforeEach(function () {
        popup = App.showHostsTableListPopup(item.header, item.hostName, item.items);
        popupBody = popup.bodyClass.create();
      });

      it('header is valid', function () {
        expect(popup.header).to.equal(item.header);
      });
      it('hostName is valid', function () {
        expect(popupBody.get('hostName')).to.equal(item.hostName);
      });
      it('items are valid', function () {
        expect(popupBody.get('items')).to.eql(item.items);
      });
      it('isObjectsList is valid', function () {
        expect(popupBody.get('isObjectsList')).to.equal(item.isObjectsList);
      });

    });

  });

});
