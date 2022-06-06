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

describe('App.ComboConfigWidgetView', function() {

  beforeEach(function() {
    this.view = App.ComboConfigWidgetView.create({
      initPopover: Em.K,
      movePopover: Em.K
    });
  });

  afterEach(function() {
    this.view.destroy();
    this.view = null;
  });

  describe('#convertToWidgetUnits', function() {
    var tests = [
      {
        valueAttributes: {
          entries: [
            {
              label: "Item A",
              value: "a"
            },
            {
              label: "Item B",
              value: "b"
            },
          ]
        },
        e: [
          {
            configValue: "a",
            widgetValue: "Item A"
          },
          {
            configValue: "b",
            widgetValue: "Item B"
          }
        ]
      }
    ];

    tests.forEach(function(test) {
      it('should convert {0} to {1}'.format(JSON.stringify(test.valueAttributes), JSON.stringify(test.e)), function() {
        var result = this.view.convertToWidgetUnits(test.valueAttributes);
        expect(JSON.parse(JSON.stringify(result))).to.eql(test.e);
      });
    });
  });

  describe('#generateWidgetValue', function() {
    var tests = [
      {
        valuesList: [
          Em.Object.create({
            configValue: 'a',
            widgetValue: 'Item A'
          }),
          Em.Object.create({
            configValue: 'b',
            widgetValue: 'Item B'
          })
        ],
        value: 'a',
        e: 'Item A'
      },
      {
        valuesList: [
          Em.Object.create({
            configValue: 'a',
            widgetValue: 'Item A'
          }),
          Em.Object.create({
            configValue: 'b',
            widgetValue: 'Item B'
          })
        ],
        value: 'b',
        e: 'Item B'
      }
    ];

    tests.forEach(function(test) {
      it('should convert config value: `{0}` to widget value: `{1}`'.format(test.value, test.e), function() {
        this.view.set('content', {});
        this.view.set('content.valuesList', Em.A(test.valuesList));
        this.view.set('config', {
          isValid: true,
          value: test.value
        });
        expect(this.view.generateWidgetValue(test.value)).to.be.equal(test.e);
      });
    });
  });

  describe('#generateConfigValue', function() {
    var tests = [
      {
        valuesList: [
          Em.Object.create({
            configValue: 'a',
            widgetValue: 'Item A'
          }),
          Em.Object.create({
            configValue: 'b',
            widgetValue: 'Item B'
          })
        ],
        value: 'Item A',
        e: 'a'
      },
      {
        valuesList: [
          Em.Object.create({
            configValue: 'a',
            widgetValue: 'Item A'
          }),
          Em.Object.create({
            configValue: 'b',
            widgetValue: 'Item B'
          })
        ],
        value: 'Item B',
        e: 'b'
      }
    ];

    tests.forEach(function(test) {
      it('should convert widget value: `{0}` to config value: `{1}`'.format(test.value, test.e), function() {
        this.view.set('content', {});
        this.view.set('content.valuesList', Em.A(test.valuesList));
        expect(this.view.generateConfigValue(test.value)).to.be.equal(test.e);
      });
    });
  });

  describe('#isValueCompatibleWithWidget()', function() {
    beforeEach(function() {
      this.view.set('content', {});
      this.view.set('config', {});
    });
    it('pass validation', function() {
      this.view.set('config.isValid', true);
      this.view.set('config.value', 'v1');
      this.view.set('content.valuesList', [{configValue: 'v1'}, {configValue: 'v2'}]);
      expect(this.view.isValueCompatibleWithWidget()).to.be.true;
    });

    it('fail validation by isValid', function() {
      this.view.set('config.isValid', false);
      expect(this.view.isValueCompatibleWithWidget()).to.be.false;
    });

    it('fail validation value that missing from list', function() {
      this.view.set('config.isValid', true);
      this.view.set('config.value', 'v3');
      this.view.set('content.valuesList', [{configValue: 'v1'}, {configValue: 'v2'}]);
      expect(this.view.isValueCompatibleWithWidget()).to.be.false;
    });
  });
});
